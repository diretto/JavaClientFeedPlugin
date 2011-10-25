package org.diretto.api.client.main.feed.event;

import org.diretto.api.client.main.core.entities.Comment;
import org.diretto.api.client.main.core.entities.CommentID;
import org.diretto.api.client.main.feed.FeedService;

/**
 * This interface represents a {@link Listener} for {@link FeedService} events
 * in respect of {@link Comment}s.
 * 
 * @author Tobias Schlecht
 */
public interface CommentListener extends Listener
{
	/**
	 * Called when a new {@link Comment} has been added. <br/><br/>
	 * 
	 * <i>Annotation:</i> Usually the {@link CommentID}s will be delivered in a
	 * chronological order. This means that {@code CommentID}s of earlier
	 * published {@code Comment}s will appear before {@code CommentID}s of later
	 * published {@code Comment}s. But due to concurrency there is <u>no
	 * guarantee</u> that this will always happen in the described manner.
	 * 
	 * @param commentID The {@code CommentID} of the new {@code Comment}
	 */
	void onCommentAdded(CommentID commentID);
}
