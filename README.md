
# kog [![Jitpack](https://jitpack.io/v/com.danneu/kog.svg)](https://jitpack.io/#com.danneu/kog) [![Kotlin](https://img.shields.io/badge/kotlin-1.1.1-blue.svg)](https://kotlinlang.org/) ![Heroku](https://img.shields.io/badge/heroku-ready-8b59b6.svg) [![Build Status](https://travis-ci.org/danneu/kog.svg?branch=master)](https://travis-ci.org/danneu/kog)  

A simple, experimental Kotlin web framework inspired by Clojure's Ring.

Built on top of [Jetty](http://www.eclipse.org/jetty/). 

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

## Goals

1. Simplicity
2. Middleware
3. Functional composition

## Hello World

### Basic

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

### Type-Safe Routing and Middleware

`SafeRouter` is a work-in-progress type-safe rewrite of the original naive `Router`.

``` kotlin
import com.danneu.kog.json.Encoder as JE
import com.danneu.kog.SafeRouter
import com.danneu.kog.Response
import com.danneu.kog.Request
import com.danneu.kog.Handler
import com.danneu.kog.Server

val router = SafeRouter {
    get("/", fun(): Handler = { req ->
        Response().text("homepage")
    })
    
    get("/users", fun(): Handler = { req ->
        Response().text("list users")
    })
    
    get("/users/<id>", fun(id: Int): Handler = { req ->
        Response().text("show user $id")
    })
    
    get("/users/<id>/edit", fun(id: Int): Handler = { req ->
        Response().text("edit user $id")
    })
    
    delete("/admin/users/<id>", listOf(ensureAdmin()), fun(id: Int): Handler = { req ->
        Response().text("admin panel, delete user $id")
    })
    
    get("/<a>/<b>/<c>", fun(a: Int, b: Int, c: Int): Handler = { req ->
        Response().json(JE.jsonObject("sum" to a + b + c))
    })
    
    // A route only runs if its pattern and handler arguments agree, and if the url can be coerced into the arguments.
    
    get("/hello/world", fun(a: Int, b: String): Handler = {
        Response().text("this route will *never* match since its handler expects url params that the route pattern does not specify")
    })
    
    get("/hello/world", fun(): Handler = {
        Response().text("this route *will* run")
    })
  }
}

val handler = middleware1(middleware2(middleware3(router.handler())))

fun main(args: Array<String>) {
  Server(handler).listen(3000)
}
```

### WebSockets

Note: Currently only supported by `Router`, not `SafeRouter`.

This example starts a websocket server that echoes back
to clients whatever they send the server.

``` kotlin
import com.danneu.kog.Handler
import com.danneu.kog.Server
import com.danneu.kog.Response
import com.danneu.kog.WebSocket

val echoHandler = { request: Request, socket: WebSocket ->
  val id = java.util.UUID.randomUUID()
  println("[$id] a client connected")

  socket.onError = { cause: Throwable ->
    println("[$id] onError ${cause.message}")
  }

  socket.onText = { message: String ->
    println("[$id] onText $message")
    socket.session.remote.sendString(message)
  }

  socket.onClose = { statusCode: Int, reason: String? ->
    println("[$id] onClose $statusCode ${reason ?: "<no reason>"}")
  }
}

fun main(args: Array<String>) {
  var router = Router {
    get("/") { Response().text("Hello world") }
    websocket("/ws", echoHandler)
  }
  Server(router.handler()).listen(3000)
}
```

Browser:

``` javascript
var socket = new WebSocket("ws://localhost:3000/ws")

socket.onopen = function () {
  socket.emit('hello world')
}

socket.onmessage = function (payload) {
  console.log('server said:', payload.data)
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

Response()                                     // skeleton 200 response
Response(Status.NotFound)                      // 404 response
Response.notFound()       <-- Sugar            // 404 response
Response().text("Hello")                       // text/plain
Response().html("<h1>Hello</h1>")              // text/html
Response().json(JE.jsonObject("number" to 42)) // application/json {"number": 42}
Response().json(JE.jsonArray(1, 2, 3))         // application/json [1, 2, 3]
Response().file(File("video.mp4"))             // video/mp4 (determines response headers from File metadata)
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
var handler: Handler = { request
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
import com.danneu.kog.composeMiddleware

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

### JSON Encoding

kog's built-in JSON encoder has two methods: `.jsonObject` and `.jsonArray`.

They both return a `kog.json.encode.JsonValue` object that you pass
to `Response#json`.

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.jsonObject("hello" to "world"))
  
  // Or, use the Response's convenience short-cut:
  
  Response().jsonObject("hello" to "world")
}
```

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.jsonArray("a", "b", "c"))
  
  // Or, the short-cuts:
  
  Response().jsonArray(listOf("a", "b", "c"))
  Response().jsonArray("a", "b", "c")
}
```

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.jsonObject(
    "ok" to true,
    "user" to JE.jsonObject(
      "id" to user.id,
      "username" to user.uname,
      "luckyNumbers" to JE.jsonArray(3, 9, 27)
    )
  ))
}
```

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
    Response().json(JE.jsonObject("sum" to nums.sum()))
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
  Response().json(JE.jsonObject("sum" to sum))
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
  Response().json(JE.jsonObject("success" to JE.jsonObject("uname" to uname)))
}
```

## (Old) Router

Note: `Router` is currently being replaced with `SafeRouter`,
but until then they live alongside each other.

Out of the box, kog comes with a simple but naive `Router`.

Kog's routers aren't special. They are just DSLs that spit out a handler function. 
They are optional and replaceable.

``` kotlin
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.Server
import com.danneu.kog.Router

val router = Router {
  use({ handler -> { req -> 
    // this middleware runs on every request served by this router
    println("middleware") 
    handler(req) 
  }})
  get("/") { Response().text("homepage") }
  group("/users") {
    get("/") { Response().text("list users") }
    get("/:id") { Response().text("show user") }
  }
  group("/admin", ensureAdmin()) { // ensureAdmin() only runs if routes in this group are hit
    // use(ensureAdmin())          // <-- .use() immediately inside a group has the same effect
    get("/") { Response().text("admin panel") }
  }
  // routes take optional route-level middleware varargs
  get("/foo", mw1, mw2, mw3) { Response() }
}

fun main(args: Array<String>) {
  Server(router.handler()).listen(3000)
}
```

Router methods accept optional middleware varargs:

``` kotlin
use(mw1())
use(mw1(), mw2(), mw3())
get("/") { Response() }
get("/", mw1(), mw2()) { Response() }
group("/foo") { ... }
group("/foo", mw1(), mw2()) { ... }
```

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
import com.danneu.kog.cookies.Ttl
import org.joda.time.DateTime

fun Request.parseCounter(): Int = try {
    cookies.getOrDefault("counter", "0").toInt()
} catch(e: NumberFormatException) {
    0
}

fun Response.setCounter(count: Int): Response = apply {
    cookies["counter"] = Cookie(count.toString(), duration = Ttl.Expires(DateTime().plusDays(3)))
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

## Development Logger (Middleware)

The logger middleware prints basic info about the request and response 
to stdout.

``` kotlin
import com.danneu.kog.batteries.logger

Server(logger(handler)).listen()
```

![logger screenshot](https://dl.dropboxusercontent.com/spa/quq37nq1583x0lf/_5c9x02w.png)

## Static File Serving (Middleware)

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

## Not-Modified / ETag Browser Caching (Middleware)

This middleware adds `Last-Modified` or `ETag` headers to each downstream
response which the browser will echo back on subsequent requests.

If the response's `Last-Modified`/`ETag` matches the request,
then this middleware instead responds with `304 Not Modified` which tells the
browser to use its cache.

### ETag

`notModified(etag = true)` will generate an ETag header for each
downstream response.

``` kotlin
val router = Router {
    use(notModified(etag = true))
    get("/") { Response().text("Hello, world!") }
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

### Last-Modified

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

## Multipart File Uploads (Middleware)

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

val router: Router = Router {
    get("/") {
        Response().html("""
            <!doctype html>
            <form method="POST" action="/upload" enctype="multipart/form-data">
                File1: <input type="file" name="file1">
                File2 (Ignored): <input type="file" name="file2">
                <button type="submit">Upload</button>
            </form>
        """)
    }
    post("/upload", multipart(Whitelist.only(setOf("file1")))) { req ->
        val upload = req.uploads["file1"]
        Response().text("You uploaded ${upload?.length ?: "--"} bytes")
    }
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

## HTML Templating

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
    get("/") {
        Response().html(layout(
          form().attr("enctype", "multipart/form-data").withMethod("POST").withAction("/upload").with(
            input().withType("file").withName("myFile"),
            button().withType("submit").withText("Upload File")
          )
        ))
    }
    post("/upload", multipart(Whitelist.only(setOf("myFile")))) {
        Response().text("Uploaded ${req.uploads["myFile"]?.length ?: "--"} bytes")
    }
}

fun main(args: Array<String>) {
    Server(router.handler()).listen(9000)
}
```

## WebSockets

Here's an example websocket server that upgrades the websocket request if the client has a `session_id` cookie
of value `"xxx"`:

``` kotlin
import com.danneu.kog.Handler
import com.danneu.kog.Server
import com.danneu.kog.Response
import com.danneu.kog.WebSocket

val authenticateUser: Middleware = { handler -> handler@ { req ->
    req.cookies["session_id"] != "xxx" && return@handler Response.forbidden()
    handler(req)
}}

val router = Router {
    websocket("/", authenticateUser) { request: Request, socket: WebSocket ->
        val id = java.util.UUID.randomUUID()
        println("[$id] a client connected")
        socket.onError = { cause: Throwable -> println("[$id] onError ${cause.message}") }
        socket.onClose = { statusCode: Int, reason: String? -> println("[$id] onClose $statusCode ${reason ?: "<no reason>"}") }
        socket.onText = { message: String -> println("[$id] onText $message") }
    }
}

fun main(args: Array<String>) {
    Server(router.handler()).listen(3000)
}
```

## Basic Auth (Middleware)

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

## Passing in environment variables

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
- `Env.bool(key)`: True iff the value is `"true"`, e.g. `VALUE=true java -jar app.java`

If the parse fails, `null` is returned.

## Heroku Deploy

This example application will be called "com.danneu.kogtest".

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

mainClassName = 'com.danneu.kogtest.Main' // <--------------- CHANGE ME

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

class Main {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val handler: Handler = { Response().text("Hello, world!") }
            // Heroku gives us a system env var "PORT" that we must bind to
            Server(handler).listen(Env.int("PORT") ?: 3000)
        }
    }
}
```

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
import com.danneu.kog.SafeRouter
import com.danneu.kog.Response
import com.danneu.kog.Handler
import java.io.File
import java.util.UUID

val router = SafeRouter {
    // Upload file
    post("/", fun(): Handler = handler@ { req ->
        // Generate random ID for user's upload
        val id = UUID.randomUUID()
        
        // Ensure "pastes" directory is created
        val destFile = File(File("pastes").apply { mkdir() }, id.toString())
        
        // Move user's upload into "pastes"
        req.body.copyTo(destFile.outputStream())
        
        // If stream was empty, delete the file and scold user
        if (destFile.length() == 0L) {
            destFile.delete()
            return@handler Response.badRequest().text("Paste file required")
        }
        
        // Posterity
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


## TODO

There's so much missing that it feels silly writing a TODO list, but here are some short-term reminders.

- Idea: I've started on an uncomitted decoder inspired by my JSON decoder for doing type-safe unwrapping of things
  like the request's `query` map. I'm trying to figure out the best way to continue generalizing the idea
  to short-circuit on bad input and validation failure:

  ``` kotlin
  // Example: GET /books?author=foo&title=bar
  get("/books") { req ->
    val (author, title) = D.tuple2(
      D.get("author", D.string),
      D.get("title", D.string)
    )(req.query)
    Response().text("author=$author, title=$title")
  }
  ```
      
- Use the same JSON library for encoding and decoding JSON. I currently use
  minimal-json for parsing and kotson for encoding.
- Clean up the build step. New to Java build tools, my build.gradle is
  a series of random hacks that eventually worked rather than something
  I understand.
- I use Kotlin 1.1 unstable just for the new `typealias` feature. Once 1.1
  becomes stable I think I can get rid of some build step complexity.
- Organize project packages. I should give them deliberate thought from the
  public API side since reorganizing packages will always cause a breaking
  change.
- Organize the Jetty/Servlet code. I have no experience working with Jetty nor Servlets,
  so I copied Ring's adapter. It definitely needs some deliberate TLC.
- Figure how to handle middleware/handlers trying to read the `request.body`
  InputStream after it has already been consumed upstream. For example,
  maybe consuming it transitions it into some sort of consumed stream
  type so that you must handle that case?
- Multipart: Let handler optionally get a mapping of upload streams instead
  of temporary files so that it can handle streams directly if it wants.
