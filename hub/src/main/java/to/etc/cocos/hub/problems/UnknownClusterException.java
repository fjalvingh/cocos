package to.etc.cocos.hub.problems;

import to.etc.hubserver.protocol.ErrorCode;
import to.etc.hubserver.protocol.FatalHubException;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 23-1-19.
 */
final public class UnknownClusterException extends FatalHubException {
	public UnknownClusterException(String what) {
		super(ErrorCode.clusterNotFound, what);
	}
}
