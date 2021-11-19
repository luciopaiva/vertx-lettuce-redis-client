
# Vert.x Lettuce Redis client

This is a simple project to demonstrate how one can create a working Lettuce Redis client backed by a Vert.x thread. It leverages Lettuce's async API to ensure a non-blocking execution.

## Setup

Spin up a local Redis server running on port 6379 and execute the intended example. It may be useful to run `redis-cli monitor` on a second terminal to debug the commands as they arrive.

## Examples

* SetGetDelExample - does as the name says: sets, gets and then deletes a key;
* BatchExample - shows how to issue a batch of commands to Redis.

## But why Vert.x?

No strong reason. We just need a timer mechanism and Vert.x provides one. See `VertxRunner` for more details.

