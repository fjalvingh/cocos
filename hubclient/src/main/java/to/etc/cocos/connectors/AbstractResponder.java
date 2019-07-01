package to.etc.cocos.connectors;

import to.etc.hubserver.protocol.CommandNames;
import to.etc.util.ByteBufferInputStream;
import to.etc.util.ClassUtil;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 10-2-19.
 */
abstract public class AbstractResponder implements IHubResponder {
	static private final byte[] NULLBODY = new byte[0];

	@Override public void acceptPacket(CommandContext ctx, List<byte[]> data) throws Exception {
		Object body = decodeBody(ctx.getConnector(), ctx.getEnvelope().getDataFormat(), data);
		if(null == body) {
			body = NULLBODY;
		}
		Method m = findHandlerMethod(ctx.getEnvelope().getCommand(), body.getClass());
		if(null == m) {
			throw new ProtocolViolationException("No handler for packet command " + ctx.getEnvelope().getCommand() + " with body type " + body.getClass().getName());
		}

		if(m.getAnnotation(Synchronous.class) != null) {
			invokeCall(ctx, body, m);
		} else {
			invokeCallAsync(ctx, body, m);
		}
	}

	private void invokeCallAsync(CommandContext ctx, Object body, Method m) {
		ctx.getConnector().getExecutor().execute(() -> {
			try {
				invokeCall(ctx, body, m);
			} catch(Exception x) {
				ctx.log("Failed to execute " + m.getName() + ": " + x);
				try {
					ctx.respond(x);
				} catch(Exception xx) {
					ctx.log("Could not return protocol error: " + xx);
				}
			}
		});
	}

	private void invokeCall(CommandContext ctx, Object body, Method m) throws Exception {
		try {
			m.invoke(ctx, body);
		} catch(InvocationTargetException itx) {
			Throwable tx = itx.getTargetException();
			if(tx instanceof RuntimeException) {
				throw (RuntimeException) tx;
			} else if(tx instanceof Error) {
				throw (Error) tx;
			} else if(tx instanceof Exception) {
				throw (Exception) tx;
			} else {
				throw itx;
			}
		}
	}

	private Object decodeBody(HubConnector connector,String bodyType, List<byte[]> data) throws IOException {
		switch(bodyType) {
			case CommandNames.BODY_BYTES:
				return data;
		}

		int pos = bodyType.indexOf(':');
		if(pos == -1)
			throw new ProtocolViolationException("Unknown body type " + bodyType);
		String clzz = bodyType.substring(pos + 1);
		String sub = bodyType.substring(0, pos);

		switch(sub) {
			default:
				throw new ProtocolViolationException("Unknown body type " + bodyType);

			case CommandNames.BODY_JSON:
				Class<?> bodyClass = ClassUtil.loadClass(getClass().getClassLoader(), clzz);
				return connector.getMapper().readerFor(bodyClass).readValue(new ByteBufferInputStream(data.toArray(new byte[data.size()][])));
		}
	}

	private Method findHandlerMethod(String command, Class<?> bodyClass) {
		try {
			String name = "handle" + command;
			return getClass().getMethod(name, CommandContext.class, bodyClass);
		} catch(Exception x) {
			return null;
		}
	}
}
