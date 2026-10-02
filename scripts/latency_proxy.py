#!/usr/bin/env python3
"""Loopback TCP delay fixture. It neither parses nor changes Minecraft packets."""
import argparse
import asyncio
import time


async def relay(reader, writer, delay):
    pending = asyncio.Queue(maxsize=128)

    async def read():
        try:
            while chunk := await reader.read(65536):
                await pending.put((time.monotonic() + delay, chunk))
        finally:
            await pending.put(None)

    async def write():
        while item := await pending.get():
            due, chunk = item
            await asyncio.sleep(max(0, due - time.monotonic()))
            writer.write(chunk)
            await writer.drain()

    try:
        await asyncio.gather(read(), write())
    finally:
        writer.close()


async def main(args):
    async def connected(reader, writer):
        try:
            upstream, output = await asyncio.open_connection('127.0.0.1', args.target)
            await asyncio.gather(relay(reader, output, args.delay_ms / 1000),
                                 relay(upstream, writer, args.delay_ms / 1000))
        except (ConnectionError, asyncio.CancelledError):
            writer.close()

    server = await asyncio.start_server(connected, '127.0.0.1', args.listen)
    print(f'DUI_LATENCY_READY 127.0.0.1:{args.listen} -> 127.0.0.1:{args.target} / '
          f'{args.delay_ms}ms per direction', flush=True)
    async with server:
        await server.serve_forever()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--listen', type=int, required=True)
    parser.add_argument('--target', type=int, required=True)
    parser.add_argument('--delay-ms', type=int, default=100)
    args = parser.parse_args()
    if not (1024 <= args.listen <= 65535 and 1024 <= args.target <= 65535
            and args.listen != args.target and 0 <= args.delay_ms <= 1000):
        parser.error('Use distinct ports in 1024..65535 and delay in 0..1000ms')
    asyncio.run(main(args))
