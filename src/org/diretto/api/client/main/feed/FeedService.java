package org.diretto.api.client.main.feed;

import org.diretto.api.client.main.feed.event.AttachmentListener;
import org.diretto.api.client.main.feed.event.CommentListener;
import org.diretto.api.client.main.feed.event.DocumentListener;
import org.diretto.api.client.main.feed.event.Listener;
import org.diretto.api.client.service.Service;

/**
 * This interface represents a {@code FeedService}. <br/><br/>
 * 
 * The {@code FeedService} provides the bulk of the platform functionalities in
 * respect of the {@code Feed API}.
 * 
 * @author Tobias Schlecht
 */
public interface FeedService extends Service
{
	/**
	 * Adds the given {@link DocumentListener}.
	 * 
	 * @param documentListener A {@code DocumentListener}
	 */
	void addDocumentListener(DocumentListener documentListener);

	/**
	 * Adds the given {@link AttachmentListener}.
	 * 
	 * @param attachmentListener An {@code AttachmentListener}
	 */
	void addAttachmentListener(AttachmentListener attachmentListener);

	/**
	 * Adds the given {@link CommentListener}.
	 * 
	 * @param commentListener A {@code CommentListener}
	 */
	void addCommentListener(CommentListener commentListener);

	/**
	 * Removes the given {@link Listener}.
	 * 
	 * @param listener A {@code Listener}
	 */
	void removeListener(Listener listener);
}
