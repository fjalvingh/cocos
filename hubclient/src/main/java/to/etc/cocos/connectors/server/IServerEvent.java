package to.etc.cocos.connectors.server;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import to.etc.cocos.connectors.RemoteClient;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 07-07-19.
 */
@NonNullByDefault
public interface IServerEvent {
	IServerEventType getType();

	@Nullable
	RemoteClient getClient();
}
