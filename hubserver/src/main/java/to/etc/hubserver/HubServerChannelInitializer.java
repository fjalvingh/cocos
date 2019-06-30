package to.etc.hubserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import to.etc.hubserver.protocol.CommandNames;

/**
 * Initializer for a new channel.
 */
class HubServerChannelInitializer extends ChannelInitializer<SocketChannel> {
	private static final String PING = "ping\r\n";

	private HubServer m_server;
	private final SslContext m_sslContext;

	public HubServerChannelInitializer(HubServer server, SslContext sslContext) {
		m_server = server;
		m_sslContext = sslContext;
	}

	@Override
	protected void initChannel(SocketChannel socketChannel) throws Exception {
		SslHandler sslHandler = m_sslContext.newHandler(socketChannel.alloc());
		sslHandler.engine().setEnabledProtocols(new String[] {"TLSv1.2"});

		ChannelPipeline pipeline = socketChannel.pipeline();

		pipeline.addLast("idleStateHandler", new IdleStateHandler(m_server.getPingInterval() * 2, m_server.getPingInterval(), 0));
		pipeline.addLast(sslHandler);

		//-- decoder
		pipeline.addLast("lengthDecoder", new LengthFieldBasedFrameDecoder(HubServer.MAX_PACKET_SIZE, 0, 4, 0, 4));
		pipeline.addLast("byteDecoder", new ByteArrayDecoder());

		//-- encoder
//			pipeline.addLast("lengthEncoder", new LengthFieldPrepender(4));
			pipeline.addLast("bytesEncoder", new ByteArrayEncoder());

		pipeline.addLast("timeoutHandler", new SimpleUserEventChannelHandler<Object>() {
			@Override protected void eventReceived(ChannelHandlerContext ctx, Object evt) throws Exception {
				if(evt instanceof IdleStateEvent) {
					IdleStateEvent ie = (IdleStateEvent) evt;
					if (ie.state() == IdleState.READER_IDLE) {
						ctx.close();
						System.out.println("Disconnect requested");
					} else if (ie.state() == IdleState.WRITER_IDLE) {
//							ByteBuf buffer = ctx.alloc().buffer(6);
//							ChannelFuture future = ctx.channel().write(PING);
//							buffer.writeCharSequence(PING, Charset.defaultCharset());
//							ctx.writeAndFlush(buffer);
//							future.await();

						PacketBuilder b = new PacketBuilder(ctx.alloc(), (byte)0x00, "", m_server.getIdent(), CommandNames.PING_CMD);
						ctx.writeAndFlush(b.getCompleted());
						System.out.println("ping sent");
					}
				}
				else
					System.out.println("Event: " + evt.getClass());
			}
		});
		pipeline.addLast(new CentralSocketHandler(m_server, socketChannel));
	}

}