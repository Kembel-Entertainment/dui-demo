"""Original deterministic browser page for the optional real-client scenario."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from threading import Thread

def start():
    class Fixture(BaseHTTPRequestHandler):
        def log_message(self, *args): pass
        def do_GET(self):
            title = 'Second' if self.path.startswith('/second') else 'First'
            data = ('''<!doctype html><title>''' + title + '''</title>
<style>body{margin:0;height:2200px;background:linear-gradient(135deg,#cf3270,#235fdd,#24c798)}
h1{color:white;font:32px sans-serif;padding:20px}button{position:absolute;left:calc(50% - 50px);top:calc(50vh - 20px);width:100px;height:40px}</style>
<h1>Original DUI browser fixture</h1><button onclick="document.title='Clicked';this.textContent='Clicked'">Click me</button>
<script>onscroll=()=>document.title='Scrolled:'+Math.round(scrollY)</script>''').encode()
            self.send_response(200); self.send_header('Content-Type', 'text/html; charset=utf-8')
            self.send_header('Content-Length', str(len(data))); self.end_headers(); self.wfile.write(data)
    server = ThreadingHTTPServer(('127.0.0.1', 0), Fixture)
    Thread(target=server.serve_forever, daemon=True).start()
    return server, 'http://127.0.0.1:' + str(server.server_port) + '/'
