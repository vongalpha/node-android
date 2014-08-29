package com.iwebpp.node;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;

import android.util.Log;

import com.iwebpp.node.Readable2.State;

public final class WrapReadable2 extends Readable2 {
	private final static String TAG = "WrapReadable2";

	private Readable stream;

	private State state;
	boolean paused;

	public WrapReadable2(Options options, Readable oldstream) {
		super(options);
		// TODO Auto-generated constructor stub
		stream = oldstream;
		state = _readableState;
		paused = false;
		
		final Readable2 self = this;
		stream.on("end", new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) throws Throwable {
				Log.d(TAG, "wrapped end");

				if (state.getDecoder()!=null && !state.isEnded()) {
					/*var chunk = state.decoder.end();
			      if (chunk && chunk.length)
			        self.push(chunk);
					 */
					CharBuffer cbuf = CharBuffer.allocate(1024 * 1024);
					state.getDecoder().flush(cbuf);
					String chunk = cbuf.toString();

					if (chunk!=null && Util.chunkLength(chunk)>0) {
						self.push(chunk, null);
					}
				}

				self.push(null, null);
			}
		});

		stream.on("data", new EventEmitter.Listener() {

			@Override
			public void invoke(Object chunk) throws Throwable {
				Log.d(TAG, "wrapped data");
				if (state.getDecoder() != null)
					///chunk = state.decoder.write(chunk);
					chunk = state.getDecoder().decode((ByteBuffer) chunk);

				if (chunk==null || !state.isObjectMode() && Util.chunkLength(chunk)==0)
					return;

				boolean ret = self.push(chunk, null);
				if (!ret) {
					paused = true;
					stream.pause();
				}
			}
		});

		// proxy all the other methods.
		// important when wrapping filters and duplexes.
		/*for (var i in stream) {
		    if (util.isFunction(stream[i]) && util.isUndefined(this[i])) {
		      this[i] = function(method) { return function() {
		        return stream[method].apply(stream, arguments);
		      }}(i);
		    }
		  }*/

		// proxy certain important events.
		/*var events = ["error", "close", "destroy", "pause", "resume"];
		  events.forEach(function(ev) {
		    stream.on(ev, self.emit.bind(self, ev));
		  });*/
		stream.on("error", new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) throws Throwable {
				// TODO Auto-generated method stub
				self.emit("error", data);
			}
		});
		stream.on("close", new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) throws Throwable {
				// TODO Auto-generated method stub
				self.emit("close", data);
			}
		});
		stream.on("destroy", new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) throws Throwable {
				// TODO Auto-generated method stub
				self.emit("destroy", data);
			}
		});
		stream.on("pause", new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) throws Throwable {
				// TODO Auto-generated method stub
				self.emit("pause", data);
			}
		});
		stream.on("resume", new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) throws Throwable {
				// TODO Auto-generated method stub
				self.emit("resume", data);
			}
		});
		
	}

	// when we try to consume some more bytes, simply unpause the
	// underlying stream.
	@Override
	public void _read(int n) {
		Log.d(TAG, "wrapped _read "+n);
		if (paused) {
			paused = false;
			stream.resume();
		}
	}
	
}