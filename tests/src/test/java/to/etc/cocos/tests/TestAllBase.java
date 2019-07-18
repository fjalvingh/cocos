package to.etc.cocos.tests;

import org.junit.After;
import to.etc.cocos.connectors.ConnectorState;
import to.etc.cocos.connectors.HubClient;
import to.etc.cocos.connectors.HubServer;
import to.etc.cocos.connectors.JsonPacket;
import to.etc.cocos.connectors.client.IClientPacketHandler;
import to.etc.cocos.connectors.server.IClientAuthenticator;
import to.etc.cocos.hub.Hub;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 30-6-19.
 */
public class TestAllBase {
	public static final int HUBPORT = 9890;

	public static final String CLUSTERNAME = "junit";

	public static final String SERVERNAME = "rmtserver";

	public static final String CLIENTID = "testDaemon";

	public static final String CLIENTPASSWORD = "tokodoko";

	public static final String CLUSTERPASSWORD = "inujit";

	private Hub m_hub;

	private HubClient m_client;

	private HubServer m_server;

	private String m_serverPassword;

	private String m_clientPassword;

	private String m_allClientPassword = CLIENTPASSWORD;

	public HubClient client() {
		HubClient client = m_client;
		if(null == client) {
			IClientPacketHandler ph = new IClientPacketHandler() {
				@Override public JsonPacket getInventory() {
					return new InventoryTestPacket();
				}
			};

			client = m_client = HubClient.create(ph, "localhost", HUBPORT, CLUSTERNAME, CLIENTID, m_clientPassword == null ? CLIENTPASSWORD : m_clientPassword);
			client.start();
		}
		return client;
	}

	public HubServer server() {
		HubServer server = m_server;
		if(null == server) {
			String id = SERVERNAME + "@" + CLUSTERNAME;

			IClientAuthenticator au = new IClientAuthenticator() {
				@Override public boolean clientAuthenticated(String clientId, byte[] challenge, byte[] challengeResponse, String clientVersion) throws Exception {
					return authenticateClient(clientId, challenge, challengeResponse);
				}
			};

			String pw = m_serverPassword != null ? m_serverPassword : CLUSTERPASSWORD;
			m_server = server = HubServer.create(au, "localhost", HUBPORT, pw, id);
			server.start();
		}
		return server;
	}

	private boolean authenticateClient(String clientId, byte[] challenge, byte[] response) throws Exception {
		String ref = m_allClientPassword + ":" + clientId;
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(ref.getBytes(StandardCharsets.UTF_8));
		md.update(challenge);
		byte[] digest = md.digest();
		return Arrays.equals(digest, response);
	}

	public HubServer serverConnected() {
		server().observeConnectionState()
			.doOnNext(a -> System.out.println(">> got state " + a))
			.filter(a -> a == ConnectorState.AUTHENTICATED)
			.timeout(5, TimeUnit.SECONDS)
			.blockingFirst();
		return server();
	}

	public Hub hub() throws Exception {
		Hub hub = m_hub;
		if(null == hub) {
			m_hub = hub = new Hub(HUBPORT, "testHUB", false, a -> CLUSTERPASSWORD);
			hub.startServer();
		}
		return hub;
	}

	@After
	public void tearDown() throws Exception {
		Hub hub = m_hub;
		if(null != hub) {
			m_hub = null;
			hub.terminateAndWait();
		}

		HubServer server = m_server;
		if(null != server) {
			m_server = null;
			server.terminateAndWait();
		}
		m_serverPassword = null;
		m_clientPassword = null;
	}

	protected void setServerPassword(String assword) {
		m_serverPassword = assword;
	}

	protected void setClientPassword(String clientPassword) {
		m_clientPassword = clientPassword;
	}
}
