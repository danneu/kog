# kog [![Jitpack](https://jitpack.io/v/com.danneu/kog.svg)](https://jitpack.io/#com.danneu/kog) [![Kotlin](https://img.shields.io/badge/kotlin-1.1.0%20beta-blue.svg)](https://kotlinlang.org/) [![Heroku](https://img.shields.io/badge/heroku-ready-8b59b6.svg)](#heroku-deploy) [![Build Status](https://travis-ci.org/danneu/kog.svg?branch=master)](https://travis-ci.org/danneu/kog) [![Dependency Status](https://david-dm.org/danneu/kog.svg)](https://david-dm.org/danneu/kog)

A simple, experimental Kotlin web framework inspired by Clojure's Ring.

Built on top of [Jetty](http://www.eclipse.org/jetty/).

## Goals

1. Simplicity
2. Middleware
3. Functional composition

## Table of Contents

<!-- toc -->

- [Install](#install)
- [Quick Start](#quick-start)
  * [Hello World](#hello-world)
  * [Type-Safe Routing](#type-safe-routing)
- [Concepts](#concepts)
  * [Request & Response](#request--response)
  * [Handler](#handler)
  * [Middleware](#middleware)
    + [**Tip:** Short-Circuiting Lambdas](#tip-short-circuiting-lambdas)
- [JSON](#json)
  * [JSON Encoding](#json-encoding)
  * [JSON Decoding](#json-decoding)
- [Routing](#routing)
  * [Router mounting](#router-mounting)
- [Cookies](#cookies)
  * [Request Cookies](#request-cookies)
  * [Response Cookies](#response-cookies)
- [Included Middleware](#included-middleware)
  * [Development Logger](#development-logger)
  * [Static File Serving](#static-file-serving)
  * [Conditional-Get Caching](#conditional-get-caching)
    + [ETag](#etag)
    + [Last-Modified](#last-modified)
  * [Multipart File Uploads](#multipart-file-uploads)
  * [Basic Auth](#basic-auth)
  * [Compression / Gzip](#compression--gzip)
- [HTML Templating](#html-templating)
- [WebSockets](#websockets)
- [Environment Variables](#environment-variables)
- [Heroku Deploy](#heroku-deploy)
- [Example: Tiny Pastebin Server](#example-tiny-pastebin-server)
- [License](#license)

<!-- tocstop -->

## Install

[![Jitpack](https://jitpack.io/v/com.danneu/kog.svg)](https://jitpack.io/#com.danneu/kog)

``` groovy
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile "com.danneu:kog:x.y.z"
    // Or always get latest
    compile "com.danneu:kog:master-SNAPSHOT"
}
```

## Quick Start

### Hello World

``` kotlin
import com.danneu.kog.Response
import com.danneu.kog.Request
import com.danneu.kog.Handler
import com.danneu.kog.Server

fun handler(req: Request): Response {
  return Response().html("<h1>Hello world</h1>")
}

// or use the Handler typealias:

val handler: Handler = { req ->
  Response().html("<h1>Hello world</h1>") 
}

fun main(args: Array<String>) {
  Server(handler).listen(3000)
}
```

### Type-Safe Routing

``` kotlin
import com.danneu.kog.json.Encoder as JE
import com.danneu.kog.Router
import com.danneu.kog.Response
import com.danneu.kog.Request
import com.danneu.kog.Handler
import com.danneu.kog.Server

val router = Router {
    get("/users", fun(): Handler = { req ->
        Response().text("list users")
    })
    
    get("/users/<id>", fun(id: Int): Handler = { req ->
        Response().text("show user $id")
    })
    
    get("/users/<id>/edit", fun(id: Int): Handler = { req ->
        Response().text("edit user $id")
    })
    
    // Wrap routes in a group to dry up middleware application
    group("/stories/<id>", listOf(middleware)) {
        get("/comments", listOf(middleware), fun(id: java.util.UUID): Handler = { 
            Response().text("listing comments for story $id")
        })
    }
    
    delete("/admin/users/<id>", listOf(ensureAdmin()), fun(id: Int): Handler = { req ->
        Response().text("admin panel, delete user $id")
    })
    
    get("/<a>/<b>/<c>", fun(a: Int, b: Int, c: Int): Handler = { req ->
        Response().json(JE.obj("sum" to JE.num(a + b + c)))
    })
  }
}

val handler = middleware1(middleware2(middleware3(router.handler())))

fun main(args: Array<String>) {
  Server(handler).listen(3000)
}
```

## Concepts

A kog application is simply a function that takes a `Request` and
returns a `Response`.

### Request & Response

The Request and Response have an API that makes it easy to chain
transformations together.

Example junk-drawer:

``` kotlin
import com.danneu.kog.Status
import com.danneu.kog.json.Encoder as JE
import java.util.File

Response()                                      // skeleton 200 response
Response(Status.NotFound)                       // 404 response
Response.notFound()       <-- Sugar             // 404 response
Response().text("Hello")                        // text/plain
Response().html("<h1>Hello</h1>")               // text/html
Response().json(JE.obj("number" to JE.num(42))) // application/json {"number": 42}
Response().json(JE.array(JE.num(1), JE.num(2), JE.num(3))) // application/json [1, 2, 3]
Response().file(File("video.mp4"))                 // video/mp4 (determines response headers from File metadata)
Response().stream(File("video.mp4"), "video/mp4")  // video/mp4
Response().setHeader(Header.ContentType, "application/json")
Response().appendHeader(Header.Custom("X-Fruit"), "orange")
Response().redirect("/")                           // 302 redirect
Response().redirect("/", permanent = true)         // 301 redirect
Response().redirectBack(request, "/")              // 302 redirect 
```

``` kotlin
import com.danneu.kog.json.Decoder as JD
import com.danneu.kog.Header

// GET http://example.com/users?sort=created,  json body is {"foo": "bar"}
var handler: Handler = { request ->
  request.href                     // http://example.com/users?sort=created
  request.path                     // "/users"
  request.method                   // Method.get
  request.params                   // Map<String, Any>
  request.json(decoder)            // Result<*, Exception>
  request.utf8()                   // "{\"foo\": \"bar\"}"
  request.headers                  // [(Header.Host, "example.com"), ...]
  request.getHeader(Header.Host)   // "example.com"?
  request.getHeader(Header.Custom("xxx"))                 // null
  request.setHeader(Header.UserAgent, "MyCrawler/0.0.1")  // Request
}
```

### Handler

``` kotlin
typealias Handler = (Request) -> Response
```

Your application is a function that takes a Request and returns a Response.

``` kotlin
val handler: Handler = { request in 
  Response().text("Hello world")
}

fun main(args: Array<String>) {
  Server(handler).listen(3000)
}
```

### Middleware

``` kotlin
typealias Middleware = (Handler) -> Handler
```

Middleware functions let you run logic when the request is going downstream
and/or when the response is coming upstream.

``` kotlin
val logger: Middleware = { handler -> { request ->
  println("Request coming in")
  val response = handler(request)
  println("Response going out")
  response
}}

val handler: Handler = { Response().text("Hello world") }

fun main(args: Array<String>) {
  Server(logger(handler)).listen()
}
```

Since middleware are just functions, it's trivial to compose them:

``` kotlin
import com.danneu.kog.middleware.composeMiddleware

// `logger` will touch the request first and the response last
val middleware = composeMiddleware(logger, cookieParser, loadCurrentUser)
Server(middleware(handler)).listen(3000)
```

#### **Tip:** Short-Circuiting Lambdas

You often want to bail early when writing middleware and handlers,
like short-circuiting your handler with a `400 Bad Request` when the
client gives you invalid data.

The compiler will complain if you `return` inside a lambda expression,
but you can fix this by using a `label@`:

``` kotlin
val middleware: Middleware = { handler -> handler@ { req -> 
    val data = req.query.get("data") ?: return@handler Response.badRequest()
    Response().text("You sent: $data")
}}
```

## JSON

kog wraps the small, fast, and simple [ralfstx/minimal-json][minimal-json] library
with combinators for working with JSON.

[minimal-json]: https://github.com/ralfstx/minimal-json#performance

### JSON Encoding

kog's built-in JSON encoder has these methods: `.obj()`, `.array()`, `.num()`, `.str()`, `.null()`, `.bool()`.

They all return `kog.json.encode.JsonValue` objects that you pass to `Response#json`.

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.obj("hello" to JE.str("world")))
}
```

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.array(JE.str("a"), JE.str("b"), JE.str("c")))
  // Or
  Response().json(JE.array(listOf(JE.str("a"), JE.str("b"), JE.str("c"))))
}
```

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.obj(
    "ok" to JE.bool(true),
    "user" to JE.obj(
      "id" to JE.num(user.id),
      "username" to JE.str(user.uname),
      "luckyNumbers" to JE.array(JE.num(3), JE.num(9), JE.num(27))
    )
  ))
}
```

It might seem redundant/tedious to call `JE.str("foo")` and `JE.num(42)`, but it's type-safe so that
you can only pass things into the encoder that's json-serializable. I'm not sure if kotlin supports
anything simpler at the moment.

### JSON Decoding

kog comes with a declarative JSON parser combinator inspired by Elm's.

`Decoder<T>` is a decoder that will return `Result<T, Exception>` when
invoked on a JSON string.

Check out <https://github.com/kittinunf/Result> for more info about the
Result object.

``` kotlin
import com.danneu.kog.json.Decoder as JD
import com.danneu.kog.json.Encoder as JE

// example request payload: [1, 2, 3]
val handler = { request ->
  request.json(JD.array(JD.int)).fold({ nums ->
    // success
    Response().json(JE.obj("sum" to JE.num(nums.sum())))
  }, { parseException -> 
    // failure
    Response.badRequest()
  })
}
```

We can use `Result#getOrElse()` to rewrite the previous example so that
invalid user-input will defaults to an empty list of numbers.

``` kotlin
import com.danneu.kog.json.Decoder as JD
import com.danneu.kog.json.Encoder as JE

// example request payload: [1, 2, 3]
val handler = { req ->
  val sum = req.json(JD.array(JD.int)).getOrElse(emptyList()).sum()
  Response().json(JE.obj("sum" to JE.num(sum)))
}
```

This authentication handler parses the username/password combo from
the request's JSON body:

``` kotlin
import com.danneu.kog.json.Decoder as JD
import com.danneu.kog.json.Encoder as JE

// example request payload: {"user": {"uname": "chuck"}, "password": "secret"}
val handler = { request ->
  val decoder = JD.pair(
    JD.getIn(listOf("user", "uname"), JD.string),
    JD.get("password", JD.string)
  )
  val (uname, password) = request.json(decoder)
  // ... authenticate user ...
  Response().json(JE.obj("success" to JE.obj("uname" to JE.str(uname))))
}
```

## Routing

kog's router is type-safe because routes only match if the URL params can be parsed into
the arguments that your function expects.

Available coercions:

- `kotlin.Int`
- `kotlin.Long`
- `kotlin.Float`
- `kotlin.Double`
- `java.util.UUID`

For example:

```kotlin
Router {
    // GET /uuid/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa -> 200 Ok
    // GET /uuid/AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA -> 200 Ok
    // GET /uuid/42                                   -> 404 Not Found
    // GET /uuid/foo                                  -> 404 Not Found
    get("/uuid/<x>", fun(uuid: java.util.UUID): Handler = { req ->
        Response().text("you provided a uuid of version ${uuid.version} with a timestamp of ${uuid.timestamp}")
    })
}
```

Here's a more meandering example:

``` kotlin
import com.danneu.kog.json.Encoder as JE
import com.danneu.kog.Router
import com.danneu.kog.Response
import com.danneu.kog.Request
import com.danneu.kog.Handler
import com.danneu.kog.Server

val router = Router(middleware1(), middleware2()) {
    get("/", fun(): Handler = { req ->
        Response().text("homepage")
    })
    
    get("/users/<id>", fun(id: Int): Handler = { req ->
        Response().text("show user $id")
    })
    
    get("/users/<id>/edit", fun(id: Int): Handler = { req ->
        Response().text("edit user $id")
    })
    
    // Wrap routes in a group to dry up middleware application
    group("/stories/<id>", listOf(middleware)) {
        get("/comments", listOf(middleware), fun(id: java.util.UUID): Handler = { 
            Response().text("listing comments for story $id")
        })
    }
    
    delete("/admin/users/<id>", listOf(ensureAdmin()), fun(id: Int): Handler = { req ->
        Response().text("admin panel, delete user $id")
    })
    
    get("/<a>/<b>/<c>", fun(a: Int, b: Int, c: Int): Handler = { req ->
        Response().json(JE.obj("sum" to JE.num(a + b + c)))
    })
    
    get("/hello/world", fun(a: Int, b: String): Handler = {
        Response().text("this route can never match the function (Int, Int) -> ...")
    })
    
    get("/hello/world", fun(): Handler = {
        Response().text("this route *will* match")
    })
  }
}

fun main(args: Array<String>) {
  Server(handler).listen(3000)
}
```

### Router mounting

`Router#mount(subrouter)` will merge a child router into the current router.

Useful for breaking your application into individual routers that you then mount into a top-level router.

```kotlin
val subrouter = Router {
    get("/foo", fun(): Handler = { Response() })
}

val router = Router {
    mount(subrouter)
}
```

```text
curl http://localhost:3000/foo      # 200 Ok
```

Or mount routers at a prefix:

```kotlin
val subrouter = Router {
    get("/foo", fun(): Handler = { Response() })
}

val router = Router {
    mount("/subrouter", subrouter)
}
```

```text
curl http://localhost:3000/foo              # 404 Not Found
curl http://localhost:3000/subrouter/foo    # 200 Ok
```

Or mount routers in a group:

```kotlin
val subrouter = Router {
    get("/foo", fun(): Handler = { Response() })
}

val router = Router {
    group("/group") {
        mount("/subrouter", subrouter)
    }
}
```

**Note:** The mount prefix must be static. It does not support dynamic patterns like "/users/<id>".

## Cookies

### Request Cookies

`Request#cookies` is a `MutableMap<String, String>` which maps cookie names
to cookie values received in the request.

### Response Cookies

`Response#cookies` is a `MutableMap<String, Cookie>` which maps cookie names
to cookie objects that will get sent to the client.

Here's a handler that increments a counter cookie on every request that
will expire in three days:

``` kotlin
import com.danneu.kog.Response
import com.danneu.kog.Handler
import com.danneu.kog.Server
import com.danneu.kog.cookies.Cookie
import java.time.OffsetDateTime

fun Request.parseCounter(): Int = try {
    cookies.getOrDefault("counter", "0").toInt()
} catch(e: NumberFormatException) {
    0
}

fun Response.setCounter(count: Int): Response = apply {
    cookies["counter"] = Cookie(count.toString(), duration = Cookie.Ttl.Expires(OffsetDateTime.now().plusDays(3)))
}

val handler: Handler = { request ->
    val count = request.parseCounter() + 1
    Response().text("count: $count").setCounter(count)
}

fun main(args: Array<String>) {
  Server(handler).listen(9000)
}
```

Demo:

```
$ http --session=kog-example --body localhost:9000
count: 1
$ http --session=kog-example --body localhost:9000
count: 2
$ http --session=kog-example --body localhost:9000
count: 3
```

## Included Middleware

The `com.danneu.kog.batteries` package includes some useful middleware.

### Development Logger

The logger middleware prints basic info about the request and response 
to stdout.

``` kotlin
import com.danneu.kog.batteries.logger

Server(logger(handler)).listen()
```

![logger screenshot](https://dl.dropboxusercontent.com/spa/quq37nq1583x0lf/_5c9x02w.png)

### Static File Serving

The serveStatic middleware checks the `request.path` against a directory
that you want to serve assets from.

``` kotlin
import com.danneu.kog.batteries.serveStatic

val middleware = serveStatic("./public")
val handler = { Response().text(":)") }

Server(middleware(handler)).listen()
```

If we have a `./public` folder in our project root with a file 
`./public/message.txt`, then the responses will look like this:

    $ http localhost:3000/foo
    HTTP/1.1 404 Not Found

    $ http localhost:3000/message.txt
    HTTP/1.1 200 OK
    Content-Length: 38
    Content-Type: text/plain

    This is a message from the file system

    $ http localhost:3000/../passwords.txt
    HTTP/1.1 400 Bad Request

### Conditional-Get Caching 

This middleware adds `Last-Modified` or `ETag` headers to each downstream
response which the browser will echo back on subsequent requests.

If the response's `Last-Modified`/`ETag` matches the request,
then this middleware instead responds with `304 Not Modified` which tells the
browser to use its cache.

#### ETag

`notModified(etag = true)` will generate an ETag header for each
downstream response.

``` kotlin
val router = Router(notModified(etag = true)) {
    get("/", fun(): Handler = { 
        Response().text("Hello, world!) 
    })
}
```

First request gives us an ETag.

``` bash
$ http localhost:9000
HTTP/1.1 200 OK
Content-Length: 13
Content-Type: text/plain
ETag: "d-bNNVbesNpUvKBgtMOUeYOQ"

Hello, world!
```

When we echo back the ETag, the server lets us know that
the response hasn't changed:

``` bash
$ http localhost:9000 If-None-Match:'"d-bNNVbesNpUvKBgtMOUeYOQ"'
HTTP/1.1 304 Not Modified
```

#### Last-Modified

`notModified(etag = false)` will only add a `Last-Modified` header
to downstream responses if `response.body is ResponseBody.File` since
kog can read the mtime from the File's metadata.

If the response body is not a `ResponseBody.File` type, then no header
will be added.

This is only useful for serving static assets from the filesystem since
ETags are unnecessary to generate when you have a file's modification time.

``` kotlin
val router = Router {
    // TODO: kog doesn't yet support mounting middleware on a prefix
    use("/assets", notModified(etag = false), serveStatic("public"))
    get("/") { Response().text("homepage")
}
```

### Multipart File Uploads 

To handle file uploads, use the `com.danneu.kog.batteries.multipart` middleware.

This middleware parses file uploads out of `"multipart/form-data"` requests
and populates `request.uploads : MutableMap<String, SavedUpload>` for your
handler to access which is a mapping of field names to `File` representations.

``` kotlin
package com.danneu.kog.batteries.multipart

class SavedUpload(val file: java.io.File, val filename: String, val contentType: String, val length: Long)
```

In this early implementation, by the time your handler is executed, the
file uploads have already been piped into temporary files in the file-system
which will get automatically deleted.

``` kotlin
import com.danneu.kog.Router
import com.danneu.kog.batteries.multipart
import com.danneu.kog.batteries.multipart.Whitelist

val router = Router {
    get("/", fun(): Handler = {
        Response().html("""
            <!doctype html>
            <form method="POST" action="/upload" enctype="multipart/form-data">
                File1: <input type="file" name="file1">
                File2 (Ignored): <input type="file" name="file2">
                <button type="submit">Upload</button>
            </form>
        """)
    })
    post("/upload", multipart(Whitelist.only(setOf("file1"))), fun(): Handler = { req ->
        val upload = req.uploads["file1"]
        Response().text("You uploaded ${upload?.length ?: "--"} bytes")
    })
}

fun main(args: Array<String>) {
    Server(router.handler()).listen(3000)
}
```

Pass a whitelist into `multipart()` to only process field names that
you expect.


``` kotlin
import com.danneu.kog.batteries.multipart
import com.danneu.kog.batteries.multipart.Whitelist

multipart(whitelist = Whitelist.all)
multipart(whitelist = Whitelist.only(setOf("field1", "field2")))
```

### Basic Auth

Just pass a `(name, password) -> Boolean` predicate to the
`basicAuth()` middleware. 

Your handler won't get called unless the user satisfies it.

``` kotlin
import com.danneu.kog.batteries.basicAuth

fun String.sha256(): ByteArray {
    return java.security.MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
}

val secretHash = "a man a plan a canal panama".sha256()

fun isAuthenticated(name: String, pass: String): Boolean {
    return java.util.Arrays.equals(pass.sha256(), secretHash)
}

val router = Router {
    get("/", basicAuth(::isAuthenticated)) {
        Response().text("You are authenticated!")
    }
}
```

### Compression / Gzip 

The `compress` middleware reads and manages the appropriate headers to determine if it should
send a gzip-encoded response to the client.

Options:

- `compress(threshold: ByteLength)` 
   (Default = 1024 bytes)
   Only compress the response if it is at least this large.
- `compress(predicate = (String?) -> Boolean)` 
   (Default = Looks up mime in <https://github.com/jshttp/mime-db> file)
   Only compress the response if its Content-Type header passes `predicate(type)`.
   
Some examples:

``` kotlin
import com.danneu.kog.batteries.compress
import com.danneu.kog.ByteLength

val router = Router() {
    // These responses will be compressed if they are JSON of any size
    group(compress(threshold = ByteLength.zero, predicate = { it == "application/json" })) {
        get("/a", fun(): Handler = { Response().text("foo") })          // <-- Not compressed (not json)
        get("/b", fun(): Handler = { Response().html("<h1>bar</h1>") }) // <-- Not compressed (not json)
        get("/c", fun(): Handler = { Response().jsonArray(1, 2, 3) })   // <-- Compressed
    }
    
    // These responses will be compressed if they are at least 1024 bytes
    group(compress(threshold = ByteLength.ofBytes(1024))) {
        get("/d", fun(): Handler = { Response().text("qux") })          // <-- Not compressed (too small)
    }
}
```

## HTML Templating

Templating libraries generally generate an HTML string. Just pass it to `Response().html(html)`.

The j2html library works well with the minimal theme.

    compile "com.j2html:j2html:0.7"

Here's an example server with a "/" route that renders a file-upload form that posts to a "/upload" route.

``` kotlin
import j2html.TagCreator.*
import j2html.tags.ContainerTag
import com.danneu.kog.Router
import com.danneu.kog.Response
import com.danneu.kog.Server
import com.danneu.kog.batteries.multipart
import com.danneu.kog.batteries.multipart.Whitelist

fun layout(vararg tags: ContainerTag): String = document().render() + html().with(
  body().with(*tags)
).render()

val router: Router = Router {
    get("/", fun(): Handler = {
        Response().html(layout(
          form().attr("enctype", "multipart/form-data").withMethod("POST").withAction("/upload").with(
            input().withType("file").withName("myFile"),
            button().withType("submit").withText("Upload File")
          )
        ))
    }) 
    post("/upload", multipart(Whitelist.only(setOf("myFile"))), fun(): Handler = {
        Response().text("Uploaded ${req.uploads["myFile"]?.length ?: "--"} bytes")
    }) 
}

fun main(args: Array<String>) {
    Server(router.handler()).listen(9000)
}
```

## WebSockets

Check out [examples/websockets.kt][examples-websockets] for a websocket example that demonstrates 
a websocket handler that echos back every message, and a websocket handler bound to a dynamic `/ws/<number>` route.

Take note of a few limitations explained in the comments that I'm working on fixing.

[examples-websockets]: https://github.com/danneu/kog/blob/master/src/main/kotlin/com/danneu/kog/examples/websockets.kt

## Environment Variables

Kog's `Env` object provides a central way to access any customizations passed into an application.

First it reads from an optional `.env` file, then it reads from system properties, and finally it reads
from system environment variables (highest precedence). Any conflicts will be overwritten in that order.

For instance, if we had `PORT=3000` in an `.env` file and then launched our application with:

    PORT=9999 java -jar app.java

Then this is what we'd see in our code:

``` kotlin
import com.danneu.kog.Env

Env.int("PORT") == 9999
```

For example, when deploying an application to Heroku, you want to bind to the port that Heroku gives you
via the `"PORT"` env variable. But you may want to default to port 3000 in development when there is no port
configured:

``` kotlin
import com.danneu.kog.Server
import com.danneu.kog.Env

fun main(args: Array<String>) {
    Server(router.handler()).listen(Env.int("PORT") ?: 3000)
}
```

`Env` provides some conveniences:

- `Env.string(key)`
- `Env.int(key)`
- `Env.float(key)`
- `Env.bool(key)`: True if the value is `"true"` or `"1"`, e.g. `VALUE=true java -jar app.java`

If the parse fails, `null` is returned.

You can get a new, overridden env container with `.fork()`:

```kotlin
Env.int("PORT")                               //=> 3000
Env.fork(mapOf("PORT" to "8888")).int("PORT") //=> 8888
Env.int("PORT")                               //=> 3000
```

## Heroku Deploy

This example application will be called "com.danneu.kogtest".

I'm not sure what the minimal boilerplate is, but the following is what worked for me.

In `./system.properties`:

```
java.runtime.version=1.8
```

In `./build.gradle`:

``` groovy
buildscript {
    ext.kotlin_version = "1.1-M03"
    ext.shadow_version = "1.2.3"

    repositories {
        jcenter()
        maven { url  "http://dl.bintray.com/kotlin/kotlin-eap-1.1" }
    }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version", "com.github.jengelman.gradle.plugins:shadow:$shadow_version"
    }
}

apply plugin: 'kotlin'
apply plugin: 'com.github.johnrengelman.shadow'
apply plugin: 'application'

mainClassName = 'com.danneu.kogtest.MainKt' // <--------------- CHANGE ME

repositories {
    jcenter()
    maven { url  "http://dl.bintray.com/kotlin/kotlin-eap-1.1" }
    maven { url 'https://jitpack.io' }
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    compile 'com.danneu:kog:master-SNAPSHOT'
}

task stage(dependsOn: ['shadowJar', 'clean'])
```

In `./src/main/kotlin/com/danneu/kogtest/main.kt`:

``` kotlin
package com.danneu.kogtest

import com.danneu.kog.Env
import com.danneu.kog.Handler
import com.danneu.kog.Response
import com.danneu.kog.Server

fun main(args: Array<String>) {
    val handler: Handler = { Response().text("Hello, world!") }
    Server(handler).listen(Env.int("PORT") ?: 3000)
}
```

**Reminder:** Bind to the PORT env variable that Heroku will set.

In `./Procfile`:

```
web: java -jar build/libs/kogtest-all.jar
```

Create and push to Heroku app:

```
heroku apps:create my-app
commit -am 'Initial commit'
git push heroku master
```

## Example: Tiny Pastebin Server

I got this idea from: <https://rocket.rs/guide/pastebin/>.

This simple server will have two endpoints:

- **Upload file**: `curl --data-binary @example.txt http://localhost:3000`.
  - Uploads binary stream to a "pastes" directory on the server.
  - Server responds with JSON `{ "url": "http://localhost:3000/<uuid>" }`.
- **Fetch file**: `curl http://localhost:3000/<uuid>`.
  - Server responds with file or 404.

``` kotlin
import com.danneu.kog.Router
import com.danneu.kog.Response
import com.danneu.kog.Handler
import com.danneu.kog.util.CopyLimitExceeded
import com.danneu.kog.util.limitedCopyTo
import java.io.File
import java.util.UUID

val uploadLimit = ByteLength.ofMegabytes(10)

val router = Router {
    // Upload file
    post("/", fun(): Handler = handler@ { req ->
        // Generate random ID for user's upload
        val id = UUID.randomUUID()
        
        // Ensure "pastes" directory is created
        val destFile = File(File("pastes").apply { mkdir() }, id.toString())
        
        // Move user's upload into "pastes", bailing if their upload size is too large.
        try {
            req.body.limitedCopyTo(uploadLimit, destFile.outputStream())
        } catch(e: CopyLimitExceeded) {
            destFile.delete()
            return@handler Response.badRequest().text("Cannot upload more than ${uploadLimit.byteLength} bytes")
        }
        
        // If stream was empty, delete the file and scold user
        if (destFile.length() == 0L) {
            destFile.delete()
            return@handler Response.badRequest().text("Paste file required")
        }
        
        println("A client uploaded ${destFile.length()} bytes to ${destFile.absolutePath}")
        
        // Tell user where they can find their uploaded file
        Response().jsonObject("url" to "http://localhost:${req.serverPort}/$id")
    })
    
    // Fetch file
    get("/<id>", fun(id: UUID): Handler = handler@ { req ->
        val file = File("pastes/$id")
        if (!file.exists()) return@handler Response.notFound()
        Response().file(file)
    })
}

fun main(args: Array<String>) {
    Server(router.handler()).listen(3000)
}
```

## License

MIT
