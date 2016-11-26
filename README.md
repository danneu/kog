
# kog [![Kotlin](https://img.shields.io/badge/Kotlin-1.1.1-orange.svg)](https://kotlinlang.org/)

A simple, experimental Kotlin web framework inspired by Clojure's Ring.

Built on top of [Jetty](http://www.eclipse.org/jetty/). 

Disclaimer: I'm new to Kotlin and Java. Implementing and abandoning the start of a web framework is a hobby of mine.

## Install

TODO

## Hello World

### Basic

``` kotlin
import com.danneu.kog.Request
import com.danneu.kog.Response
import com.danneu.kog.Server

fun handler(req: Request): Response {
  return Response().html("<h1>Hello world</h1>")
}

// or, even better:

val handler: Handler = { req ->
  Response().html("<h1>Hello world</h1>") 
}

fun main(args: Array<String>) {
  Server(handler).listen(3000)
}
```

### With Routing and Middleware

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
  group("/admin") {
    use(ensureAdmin()) // only runs if routes in this group are hit
    get("/") { Response().text("admin panel") }
  }
}

val handler = middleware1(middleware2(middleware3(router.handler())))

fun main(args: Array<String>) {
  Server(handler).listen(3000)
}
```

### WebSockets

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

Non-websocket requests get routed to your kog handler.

## Goals

1. Simplicity
2. Middleware
3. Functional composition

## Concepts

A kog application is simply a function that takes a `Request` and
returns a `Response`.

### Request & Response

The Request and Response have an API that makes it easy to chain
transformations together.

Example junk-drawer:

``` kotlin
import com.danneu.kog.Status
import com.danneu.kog.Encoder as JE
import java.util.File

Response()                                     // skeleton 200 response
Response(Status.notFound)                      // 404 response
Response().text("Hello")                       // text/plain
Response().html("<h1>Hello</h1>")              // text/html
Response().json(JE.jsonObject("number" to 42)) // application/json {"number": 42}
Response().json(JE.jsonArray(1, 2, 3))         // application/json [1, 2, 3]
Response().stream(File("video.mp4"), "video/mp4")
Response().setHeader("Content-Type", "application/json")
Response().appendHeader("X-Fruit", "orange")
Response().redirect("/")                           // 302 redirect
Response().redirect("/", permanent = true)         // 301 redirect
Response().redirectBack(request, "/")              // 302 redirect 
```

``` kotlin
import com.danneu.kog.json.Decoder as JD

// GET http://example.com/users?sort=created,  json body is {"foo": "bar"}
var handler: Handler = { request
  request.url                      // http://example.com/users?sort=created
  request.path                     // "/users"
  request.method                   // Method.get
  request.json(decoder)            // Result<*, Exception>
  request.utf8()                   // "{\"foo\": \"bar\"}"
  request.headers                  // [("host", "example.com"), ...]
  request.getHeader("host")        // "example.com"?
  request.getHeader("xxxxx")       // null
  request.setHeader("key", "val")  // Request
}
```

### Handler = (Request) -> Response

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

### Middleware = (Handler) -> Handler

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

## JSON

### JSON Encoding

kog's built-in JSON encoder has two methods: `.jsonObject` and `.jsonArray`.

They both return a `kog.json.encode.JsonValue` object that you pass
to `Response#json`.

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.jsonObject("hello" to "world"))
}
```

``` kotlin
import com.danneu.kog.json.Encoder as JE

val handler: Handler = { req ->
  Response().json(JE.jsonArray("a", "b", "c"))
  // or
  Response().json(JE.jsonArray(listOf("a", "b", "c")))
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
    Response(Status.badRequest)
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

## Router

Out of the box, kog comes with a simple but naive router.

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
  group("/admin") {
    use(ensureAdmin()) // only runs if routes in this group are hit
    get("/") { Response().text("admin panel") }
  }
  // routes takes optional route-level middleware varargs
  get("/foo", mw1, mw2, mw3) { Response() }
}

fun main(args: Array<String>) {
  Server(router.handler()).listen(3000)
}
```

## Development Logger (Middleware)

The logger middleware prints basic info about the request and response 
to stdout.

``` kotlin
import com.danneu.kog.batteries.logger

Server(logger(handler)).listen()
```

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

## Multipart File Uploads (Middleware)

Check out the following HTML Templating section for an example.

## HTML Templating

The j2html library works well with the minimal theme.

    compile "com.j2html:j2html:0.7"

Here's an example server with a "/" route that renders a file-upload form that posts to a "/upload" route.

``` kotlin
import j2html.TagCreator.*
import com.danneu.kog.Router
import com.danneu.kog.Response
import com.danneu.kog.Server
import com.danneu.kog.batteries.multipart

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
    post("/upload") { req ->
        Response().text("Upload: ${req.uploads["myFile"]}")
    }
}

fun main(args: Array<String>) {
    val middleware = composeMiddleware(
      multipart()
    )
    val server = Server(middleware(router.handler()))
    server.listen(9000)
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

val authenticateUser: Middleware = { handler -> fun(req: Request): Response {
    req.cookies["session_id"] != "xxx" && return Response(Status.forbidden)
    return handler(req)
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

## Performance

Just for fun, here's how kog's hello world benchmarks next to Node.js' hello world.

Kog is written naively with no thought yet given to performance, so this is a testament to how amazing Jetty is
since kog is doing more work than the Node.js example.

Node.js:

``` javascript
require('http').createServer((req, res) => res.end('Hello, World!')).listen(3000)
```

Kog:

``` kotlin
class Main {
  companion object {
    @JvmStatic fun main(args: Array<String>) {
      Server({ Response().text("Hello, World!") }).listen(9000)
    }
  }
}
```

Benching with 2 threads and 1,000 concurrent connections:

| Platform | Benchmark                           | Requests     | Per Second     |
| -------- | ----------------------------------- | ------------ | -------------- |
| Node.js  | `wrk -c 1000 http://localhost:3000` | 120,000 reqs | 12,000 req/sec |
| Kog      | `wrk -c 1000 http://localhost:9000` | 350,000 reqs | 35,000 req/sec |

## TODO

There's so much missing that it feels silly writing a TODO list, but here are some short-term reminders.

- Router should parse :params from the URL.

  ``` kotlin
  get("/users/:id") { req -> 
    Response().text("User ${req.params.get(":id")}") 
  }
  ```

  Bonus points for type safety.
- Look into Kotlin's `inline` functionality for handlers.
- I've started on an uncomitted decoder inspired by my JSON decoder for doing type-safe unwrapping of things
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
- Implement response cookies.
- Finish enumerating Status.kt codes.
- Investigate having a ./gradle/wrapper folder. Seems every Kotlin project has this which I assume packages the
  gradle build step dependency with the project instead of relying on system gradle?
- Figure how to handle middleware/handlers trying to read the `request.body`
  InputStream after it has already been consumed upstream. For example,
  maybe consuming it transitions it into some sort of consumed stream
  type so that you must handle that case?
