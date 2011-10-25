package org.diretto.api.client.main.feed.subscriber;

import org.diretto.api.client.main.feed.FeedService;

import com.sun.syndication.feed.atom.Feed;

/**
 * This interface represents a handler for the main {@link FeedService} events.
 * 
 * @author Tobias Schlecht
 */
public interface FeedHandler
{
	/**
	 * Called when a {@link Feed} update has been arrived.
	 * 
	 * @param feedURL The corresponding {@code Feed} {@code URL} in
	 *        {@code String} representation
	 * @param feed The {@code Feed} update
	 */
	void onFeedUpdate(String feedURL, Feed feed);
}
