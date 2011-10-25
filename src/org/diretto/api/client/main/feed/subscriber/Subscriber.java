package org.diretto.api.client.main.feed.subscriber;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.configuration.XMLConfiguration;
import org.diretto.api.client.base.annotations.InvocationLimited;
import org.diretto.api.client.main.feed.FeedService;
import org.diretto.api.client.main.feed.FeedServiceImpl;
import org.diretto.api.client.util.InvocationUtils;
import org.diretto.api.client.util.NetworkUtils;

import com.sun.syndication.feed.atom.Feed;

/**
 * This class represents the main part of the {@code Subscriber} role at the
 * {@code PubSubHubbub} protocol and offers the functionalities to subscribe and
 * unsubscribe {@link Feed}s.
 * 
 * @author Tobias Schlecht
 */
public class Subscriber extends com.google.pubsubhubbub.java.subscriber.Subscriber
{
	private final String hostAddress;

	/**
	 * The constructor is {@code private} to have strict control what instances
	 * exist at any time. Instead of the constructor the {@code public}
	 * <i>static factory method</i>
	 * {@link #getInstance(XMLConfiguration, FeedHandler)} returns the instances
	 * of the class.
	 * 
	 * @param xmlConfiguration The {@code XMLConfiguration} object
	 * @param feedHandler The corresponding {@code FeedHandler}
	 */
	private Subscriber(XMLConfiguration xmlConfiguration, final FeedHandler feedHandler)
	{
		super(new WebServer(xmlConfiguration, new PushHandler()
		{
			@Override
			void handleFeedUpdate(String feedURL, Feed feed)
			{
				feedHandler.onFeedUpdate(feedURL, feed);
			}
		}));

		String automationPage = xmlConfiguration.getString("pubsubhubbub/subscriber-network-settings/automation-page");

		URL automationPageURL = null;

		try
		{
			automationPageURL = new URL(automationPage);
		}
		catch(MalformedURLException e)
		{
			e.printStackTrace();
		}

		String hostIPAddress = NetworkUtils.getGlobalIPAddress(automationPageURL);

		hostAddress = "http://" + hostIPAddress + ":" + xmlConfiguration.getInt("pubsubhubbub/subscriber-network-settings/port-number");
	}

	/**
	 * Returns a {@link Subscriber} instance for the given
	 * {@link XMLConfiguration} and the corresponding {@link FeedHandler}.
	 * 
	 * @param xmlConfiguration The {@code XMLConfiguration} object
	 * @param feedHandler The corresponding {@code FeedHandler}
	 * @return A {@code Subscriber} instance
	 */
	@InvocationLimited(legitimateInvocationClasses = {FeedServiceImpl.class})
	public static synchronized Subscriber getInstance(XMLConfiguration xmlConfiguration, FeedHandler feedHandler)
	{
		String warningMessage = "The method invocation \"" + Subscriber.class.getCanonicalName() + ".getInstance(XMLConfiguration, FeedHandler)\" is not intended for this usage. Use the \"" + FeedService.class.getCanonicalName() + "\" for the feed functionalities.";
		InvocationUtils.checkMethodInvocation(warningMessage, "getInstance", XMLConfiguration.class, FeedHandler.class);

		return new Subscriber(xmlConfiguration, feedHandler);
	}

	/**
	 * Subscribes for the {@link Feed} with the specified {@code Feed}
	 * {@code URL} ({@code String} representation) at the hub with the given hub
	 * {@code URL} ({@code String} representation).
	 * 
	 * @param hubURL A hub {@code URL} ({@code String} representation)
	 * @param feedURL A {@code Feed} {@code URL} ({@code String} representation)
	 * @throws Exception
	 */
	public void subscribe(String hubURL, String feedURL) throws Exception
	{
		subscribe(hubURL, feedURL, hostAddress, null, null);
	}

	/**
	 * Unsubscribes the {@link Feed} with the specified {@code Feed} {@code URL}
	 * ({@code String} representation) at the hub with the given hub {@code URL}
	 * ({@code String} representation).
	 * 
	 * @param hubURL A hub {@code URL} ({@code String} representation)
	 * @param feedURL A {@code Feed} {@code URL} ({@code String} representation)
	 * @throws Exception
	 */
	public void unsubscribe(String hubURL, String feedURL) throws Exception
	{
		unsubscribe(hubURL, feedURL, hostAddress, null);
	}

	@Override
	public int subscribe(String hubURL, String feedURL, String hostAddress, String verifyToken, String leaseSeconds) throws Exception
	{
		return super.subscribe(hubURL, feedURL, hostAddress, verifyToken, leaseSeconds);
	}

	@Override
	public int unsubscribe(String hubURL, String feedURL, String hostAddress, String verifyToken) throws Exception
	{
		return super.unsubscribe(hubURL, feedURL, hostAddress, verifyToken);
	}
}
