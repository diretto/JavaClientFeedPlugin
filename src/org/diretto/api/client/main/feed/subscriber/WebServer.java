package org.diretto.api.client.main.feed.subscriber;

import org.apache.commons.configuration.XMLConfiguration;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.google.pubsubhubbub.java.subscriber.Web;

/**
 * This class represents an important part of the {@link Subscriber} role at the
 * {@code PubSubHubbub} protocol. A {@code WebServer} is thereby necessary to
 * enable the processing of the incoming HTTP POST requests of the corresponding
 * hub.
 * 
 * @author Tobias Schlecht
 */
class WebServer extends Web
{
	private static final String CONTEXT_PATH = "/push";

	private Server webServer = null;

	/**
	 * Constructs a {@link WebServer}.
	 * 
	 * @param xmlConfiguration The {@code XMLConfiguration} object
	 * @param pushHandler A {@code PushHandler}
	 */
	WebServer(XMLConfiguration xmlConfiguration, PushHandler pushHandler)
	{
		super(xmlConfiguration.getInt("pubsubhubbub/subscriber-network-settings/port-number"));

		webServer = new Server(getPort());

		ContextHandler contextHandler = new ContextHandler();

		contextHandler.setContextPath(CONTEXT_PATH);
		contextHandler.setResourceBase(".");
		contextHandler.setClassLoader(Thread.currentThread().getContextClassLoader());

		webServer.setHandler(contextHandler);

		contextHandler.setHandler(pushHandler);

		try
		{
			webServer.start();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void setup()
	{
		// Just in order to avoid the invocation of the setup() method of the
		// superclass
	}
}
