
# Vert.x Lettuce Redis client

This is a simple project to demonstrate how one can create a working Lettuce Redis client backed by a Vert.x thread. It leverages Lettuce's async API to ensure a non-blocking execution.

## Setup

Spin up a local Redis server running on port 6379 and execute the intended example. It may be useful to run `redis-cli monitor` on a second terminal to debug the commands as they arrive.

## Examples

* SetGetDelExample - does as the name says: sets, gets and then deletes a key;
* BatchExample - shows how to issue a batch of commands to Redis.

## But why Vert.x?

No strong reason. We just need a timer mechanism and Vert.x provides one. See `VertxRunner` for more details.

## Throughput test

To investigate what level of throughput can we achieve with Lettuce, I started a single-server ElastiCache Redis backed by an m6g.large instance and wrote a few clients to stress it in different ways.

### ThroughputSyncSingleThreadExample

This initial test runs a single thread with Lettuce in synchronized mode. The next command is only executed after the current one is complete. This means waiting a complete round trip to the Redis server and back. This test kept a steady 5 or 6 iterations per second, meaning a round trip time of about 160ms.

### ThroughputAsyncSingleThreadExample

This test is almost exactly equal to the previous one, but with one significant difference: it runs in async mode, and each command is executed in fire-and-forget fashion. By doing SSH port-forwarding, we can run `redis-cli --stat` on the server and see the requests arriving:

```
------- data ------ --------------------- load -------------------- - child -
keys       mem      clients blocked requests            connections
1          4.49M    4       0       132185 (+23255)     15
1          4.45M    4       0       152270 (+20085)     15
1          4.45M    4       0       174469 (+22199)     15
1          4.45M    4       0       199838 (+25369)     15
1          4.45M    4       0       228379 (+28541)     15
1          4.45M    4       0       256920 (+28541)     15
1          4.47M    5       0       277005 (+20085)     15
1          4.51M    5       0       300260 (+23255)     15
1          4.47M    5       0       324573 (+24313)     15
1          4.51M    5       0       354171 (+29598)     15
1          4.51M    5       0       384826 (+30655)     15
1          4.51M    5       0       427108 (+42282)     15
1          4.51M    5       0       490531 (+63423)     15
1          4.51M    5       0       587539 (+97008)     15
1          4.47M    5       0       587820 (+281)       15
1          4.49M    4       0       587821 (+1)         15
1          4.45M    4       0       587822 (+1)         15
1          4.45M    4       0       647875 (+60053)     15
1          4.49M    4       0       755826 (+107951)    15
1          4.49M    4       0       795994 (+40168)     15
1          4.49M    4       0       839333 (+43339)     15
1          4.45M    4       0       882672 (+43339)     15
1          4.45M    4       0       883336 (+664)       15
1          4.45M    4       0       883337 (+1)         15
1          4.45M    4       0       883338 (+1)         15
1          4.49M    4       0       932041 (+48703)     15
1          4.49M    4       0       1000749 (+68708)    15
1          4.49M    4       0       1073925 (+73176)    15
1          4.45M    4       0       1073926 (+1)        15
1          4.45M    4       0       1073927 (+1)        15
1          4.45M    4       0       1073928 (+1)        15
1          4.45M    4       0       1073929 (+1)        15
1          4.45M    4       0       1073930 (+1)        15
1          4.45M    4       0       1175874 (+101944)   15
1          4.45M    4       0       1175875 (+1)        15
1          4.49M    4       0       1175876 (+1)        15
1          4.45M    4       0       1175877 (+1)        15
1          4.45M    4       0       1175878 (+1)        15
1          4.45M    4       0       1175879 (+1)        15
1          4.45M    4       0       1276507 (+100628)   15
1          4.45M    4       0       1276508 (+1)        15
1          4.45M    4       0       1276509 (+1)        15
1          4.45M    4       0       1276510 (+1)        15
1          4.45M    4       0       1276511 (+1)        15
1          4.45M    4       0       1276512 (+1)        15
1          4.45M    4       0       1276513 (+1)        15
1          4.49M    4       0       1413961 (+137448)   15
1          4.45M    4       0       1414288 (+327)      15
1          4.45M    4       0       1414289 (+1)        15
1          4.45M    4       0       1414290 (+1)        15
1          4.45M    4       0       1414291 (+1)        15
1          4.45M    4       0       1414292 (+1)        15
```

Notice how it was able to keep at least 20k requests per second during the few initial seconds, but then it enters an erratic state where the rate drops to zero periodically.

A rate of 20k/s means about 50us per request. It got to 100k/s at some moments, so 10us at best.

On the Lettuce side, however, we were seeing a huge number of reported iterations per second:

```
21:26:14.483 [INFO] (main) examples.ThroughputAsyncSingleThreadExample: Iterations per second: 192948
21:26:15.484 [INFO] (main) examples.ThroughputAsyncSingleThreadExample: Iterations per second: 340928
21:26:16.903 [INFO] (main) examples.ThroughputAsyncSingleThreadExample: Iterations per second: 301589
21:26:17.905 [INFO] (main) examples.ThroughputAsyncSingleThreadExample: Iterations per second: 436323
21:26:18.906 [INFO] (main) examples.ThroughputAsyncSingleThreadExample: Iterations per second: 228783
21:26:19.907 [INFO] (main) examples.ThroughputAsyncSingleThreadExample: Iterations per second: 350194
```

Open question: what happens with the 100k+ futures being created per second that are not turning into actual requests? Are they being enqueued somewhere inside Lettuce? Where? One hypothesis is that they are being enqueued and not served in time, thus timing out.

## ThroughputAsyncSingleThreadExample2

This example explores something I found while reading [the docs](https://lettuce.io/core/snapshot/reference/#faq.timeout) with respect to `RedisCommandTimeoutException` exceptions. It says there that timeouts may be caused by tasks blocking the event loop. The event loop here is Netty's event loop, one of Lettuce's dependencies. Netty uses an nio event loop, which Lettuce uses to dispatch requests to Redis.

I am interested in this because I've seen these timeouts in production, so I am suspecting that they were possibly being caused by tasks blocking the loop since the server was not under heavy stress when the timeouts happened.

So this test executes 10 commands one immediately after the other, but when the responses arrive, each task is programmed to wait 1 second *inside the event loop*. This is what happens:

```
22:53:57.077 [INFO] (metrics) common.MetricReporter: iterations=10 timeouts=0 successes=0
22:53:58.082 [INFO] (metrics) common.MetricReporter: iterations=0 timeouts=9 successes=1
```

All 10 commands were immediately sent to Redis (confirmed by checking the server with `--stat`), but when the first response arrived, the event loop was blocked for long enough so that the other 9 responses timed out, even though they had already arrived!

So here I learned two things: 

- do not run stuff in the Lettuce even loop
- commands can time out even after their response have arrived

