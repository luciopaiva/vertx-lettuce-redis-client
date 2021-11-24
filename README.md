
# Vert.x Lettuce Redis client

This is a simple project to demonstrate how one can create a working Lettuce Redis client backed by a Vert.x thread. It leverages Lettuce's async API to ensure a non-blocking execution. There are also concurrency experiments to understand how Lettuce can be a bottleneck in a multi-thread scenario.

## Setup

Spin up a local Redis server running on port 6379 and execute the intended example. It may be useful to run `redis-cli monitor` on a second terminal to debug the commands as they arrive.

## Examples

* SetGetDelExample - does as the name says: sets, gets and then deletes a key;
* BatchExample - shows how to issue a batch of commands to Redis.

## But why Vert.x?

No strong reason. We just need a timer mechanism to schedule calls to Redis and Vert.x provides one. See `VertxRunner` for more details.

## Throughput test

To investigate what level of throughput can we achieve with Lettuce, I started a single-server ElastiCache Redis backed by an m6g.large instance and wrote a few clients to stress it in different ways.

### ThroughputSyncSingleThreadExample

This initial test runs a single thread with Lettuce in sync mode. The next command is only executed after the current one is complete. This means waiting a complete round trip to the Redis server and back. This test kept a steady 5 or 6 iterations per second, meaning a round trip time of about 160ms.

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

Open question: what happens with the 100k+ futures being created per second that are not turning into actual requests? Are they being enqueued somewhere inside Lettuce? Where? One hypothesis is that they are being enqueued and not served in time, thus timing out. I changed the test a bit to include timeout/success metrics:

```
12:18:43.604 [INFO] (metrics) common.MetricReporter: iterations=6771 timeouts=0 successes=0
12:18:44.609 [INFO] (metrics) common.MetricReporter: iterations=280364 timeouts=622 successes=4182
12:18:45.695 [INFO] (metrics) common.MetricReporter: iterations=270132 timeouts=175778 successes=0
12:18:46.706 [INFO] (metrics) common.MetricReporter: iterations=180262 timeouts=129343 successes=0
12:18:47.968 [INFO] (metrics) common.MetricReporter: iterations=192554 timeouts=168117 successes=0
12:18:49.587 [INFO] (metrics) common.MetricReporter: iterations=331703 timeouts=338685 successes=0
12:18:50.588 [INFO] (metrics) common.MetricReporter: iterations=430459 timeouts=441939 successes=3706
12:18:51.588 [INFO] (metrics) common.MetricReporter: iterations=76919 timeouts=150188 successes=0
12:18:52.592 [INFO] (metrics) common.MetricReporter: iterations=423772 timeouts=349194 successes=0
12:18:54.037 [INFO] (metrics) common.MetricReporter: iterations=175720 timeouts=268155 successes=0
12:18:55.038 [INFO] (metrics) common.MetricReporter: iterations=417649 timeouts=339175 successes=12629
12:18:56.039 [INFO] (metrics) common.MetricReporter: iterations=302516 timeouts=404125 successes=1130
12:18:57.043 [INFO] (metrics) common.MetricReporter: iterations=242783 timeouts=303394 successes=0
12:18:58.048 [INFO] (metrics) common.MetricReporter: iterations=203402 timeouts=227153 successes=15997
12:18:59.050 [INFO] (metrics) common.MetricReporter: iterations=237229 timeouts=196403 successes=5615
12:19:00.050 [INFO] (metrics) common.MetricReporter: iterations=210494 timeouts=237175 successes=0
12:19:01.054 [INFO] (metrics) common.MetricReporter: iterations=296857 timeouts=210984 successes=0
12:19:02.058 [INFO] (metrics) common.MetricReporter: iterations=286462 timeouts=297520 successes=0
12:19:03.059 [INFO] (metrics) common.MetricReporter: iterations=255092 timeouts=286007 successes=0
12:19:04.108 [INFO] (metrics) common.MetricReporter: iterations=238473 timeouts=245618 successes=0
12:19:05.108 [INFO] (metrics) common.MetricReporter: iterations=233009 timeouts=247258 successes=0
```

```
------- data ------ --------------------- load -------------------- - child -
keys       mem      clients blocked requests            connections
1          4.51M    5       0       6101089 (+64035)    36
1          4.47M    5       0       6129899 (+28810)    36
1          4.47M    5       0       6130490 (+591)      36
1          4.47M    5       0       6141664 (+11174)    36
1          4.47M    5       0       6182600 (+40936)    36
1          4.51M    5       0       6187650 (+5050)     36
1          4.47M    5       0       6236171 (+48521)    36
1          4.47M    5       0       6255842 (+19671)    36
1          4.51M    5       0       6303579 (+47737)    36
1          4.51M    5       0       6353433 (+49854)    36
1          4.53M    6       0       6392427 (+38994)    36
1          4.53M    6       0       6440720 (+48293)    36
1          4.53M    6       0       6485828 (+45108)    36
1          4.53M    6       0       6527935 (+42107)    36
1          4.47M    5       0       6563174 (+35239)    36
1          4.47M    5       0       6620524 (+57350)    36
1          4.47M    5       0       6671637 (+51113)    36
1          4.47M    5       0       6725949 (+54312)    36
1          4.51M    5       0       6777936 (+51987)    36
1          4.51M    5       0       6830036 (+52100)    36
1          4.51M    5       0       6884734 (+54698)    36
```

Many of the requests were ending in timeouts as expected, but are all of them either timing out or succeeding, or are they ending some other way? Are we leaking requests?

To answer the question above, I changed the test once more. Now it sends as many requests as it can, but only for 10 seconds. While doing that, I am also tracking the total count of requests, how many succeeded, how many timed out and how many are still pending. This is what I got:

```
20:13:36.091 [INFO] (metrics) common.MetricReporter: iterations=8054 timeouts=0 successes=0 total-pending=8085
20:13:37.095 [INFO] (metrics) common.MetricReporter: iterations=283368 timeouts=0 successes=26298 total-pending=265175
20:13:38.098 [INFO] (metrics) common.MetricReporter: iterations=300597 timeouts=108868 successes=8221 total-pending=448734
20:13:39.144 [INFO] (metrics) common.MetricReporter: iterations=145664 timeouts=94080 successes=201 total-pending=500096
20:13:40.976 [INFO] (metrics) common.MetricReporter: iterations=306241 timeouts=268326 successes=4083 total-pending=533924
20:13:42.762 [INFO] (metrics) common.MetricReporter: iterations=274316 timeouts=355601 successes=0 total-pending=452700
20:13:43.767 [INFO] (metrics) common.MetricReporter: iterations=387058 timeouts=454932 successes=0 total-pending=384814
20:13:45.098 [INFO] (metrics) common.MetricReporter: iterations=30267 timeouts=109998 successes=0 total-pending=305086
20:13:46.101 [INFO] (metrics) common.MetricReporter: iterations=327705 timeouts=305087 successes=1830 total-pending=325874
20:13:47.104 [INFO] (metrics) common.MetricReporter: iterations=0 timeouts=325874 successes=0 total-pending=0
20:13:48.108 [INFO] (metrics) common.MetricReporter: iterations=0 timeouts=0 successes=0 total-pending=0
```

It shows that, in the end, after requests have ceased, we end up with zero pending requests, meaning all of them resolved one way or another. Also notice that the queue is unbounded because the client was built by passing `.requestQueueSize(Integer.MAX_VALUE)`. Setting this to a small value would mean the queue would eventually get full and new requests would be denied with a RedisException (see [docs section on client options](https://lettuce.io/core/release/reference/#client-options)). 

## ThroughputAsyncSingleThreadExample2

This example explores something I found while reading [the docs](https://lettuce.io/core/snapshot/reference/#faq.timeout) with respect to `RedisCommandTimeoutException` exceptions. It says there that timeouts may be caused by tasks blocking the event loop. The event loop here is Netty's event loop, one of Lettuce's dependencies. Netty uses an nio event loop, which Lettuce uses to dispatch requests to Redis.

I am interested in this because I've seen these timeouts in production code on a project I've worked on, so I am suspecting that they were possibly being caused by tasks blocking the loop since the server was not under heavy stress when the timeouts happened and there was no other plausible explanation for the timeouts.

So this test executes 10 commands, one immediately after the other, but when the responses arrive, each task is programmed to wait 1 second *inside the event loop*. This is what happens:

```
22:53:57.077 [INFO] (metrics) common.MetricReporter: iterations=10 timeouts=0 successes=0
22:53:58.082 [INFO] (metrics) common.MetricReporter: iterations=0 timeouts=9 successes=1
```

All 10 commands were immediately sent to Redis (confirmed by checking the server with `--stat`), but when the first response arrived, the event loop was blocked for long enough so that the other 9 responses timed out, even though they had already arrived!

So here I learned two things: 

- do not run stuff in the Lettuce event loop
- commands can time out even after their response has arrived

## ThroughputAsyncSingleThreadExample3

This test runs a client that keeps a steady rate of requests to Redis.

## ThroughputAsyncMultiThreadExample

This test reuses `ThroughputAsyncSingleThreadExample3`, instancing multiple clients.
