package to.etc.cocos.connectors.server;

import org.eclipse.jdt.annotation.Nullable;
import to.etc.cocos.connectors.ifaces.IRemoteClient;
import to.etc.cocos.connectors.ifaces.IServerEvent;
import to.etc.cocos.connectors.ifaces.IServerEventType;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 07-07-19.
 */
public class ServerEventBase implements IServerEvent {
	private final IServerEventType m_type;

	@Nullable
	private final IRemoteClient m_client;

	public ServerEventBase(IServerEventType type) {
		m_type = type;
		m_client = null;
	}

	public ServerEventBase(IServerEventType type, @Nullable IRemoteClient client) {
		m_type = type;
		m_client = client;
	}

	@Override public IServerEventType getType() {
		return m_type;
	}

	@Override public IRemoteClient getClient() {
		return m_client;
	}
}
