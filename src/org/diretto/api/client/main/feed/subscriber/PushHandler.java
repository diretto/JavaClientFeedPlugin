package org.diretto.api.client.main.feed.subscriber;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import com.google.pubsubhubbub.java.subscriber.PuSHhandler;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * This {@code abstract} class extends the {@link PuSHhandler} class and
 * provides an own implementation for the incoming HTTP POST requests, which are
 * sent by the corresponding hub in the case when a subscribed {@link Feed} has
 * been updated.
 * 
 * @author Tobias Schlecht
 */
abstract class PushHandler extends PuSHhandler
{
	/**
	 * Constructs a {@link PushHandler}.
	 */
	PushHandler()
	{
		super();
	}

	/**
	 * Called when a {@link Feed} update has been arrived.
	 * 
	 * @param feedURL The corresponding {@code Feed} {@code URL} in
	 *        {@code String} representation
	 * @param feed The {@code Feed} update
	 */
	abstract void handleFeedUpdate(String feedURL, Feed feed);

	@Override
	public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException
	{
		if(httpServletRequest != null && httpServletRequest.getMethod().equals("POST"))
		{
			if(httpServletRequest.getContentType().contains("application/atom+xml"))
			{
				InputStream inputStream = httpServletRequest.getInputStream();

				try
				{
					WireFeedInput wireFeedInput = new WireFeedInput();

					final Feed feed = (Feed) wireFeedInput.build(new XmlReader(inputStream));

					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							handleFeedUpdate(feed.getId(), feed);
						}
					}).start();
				}
				catch(FeedException e)
				{
					e.printStackTrace();
				}

				httpServletResponse.setStatus(HttpServletResponse.SC_OK);
			}
			else
			{
				httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}

			httpServletResponse.setContentType("application/x-www-form-urlencoded");

			request.setHandled(true);
		}
		else
		{
			super.handle(target, request, httpServletRequest, httpServletResponse);
		}
	}
}
