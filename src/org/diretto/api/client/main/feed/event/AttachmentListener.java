package org.diretto.api.client.main.feed.event;

import org.diretto.api.client.main.core.entities.Attachment;
import org.diretto.api.client.main.core.entities.AttachmentID;
import org.diretto.api.client.main.feed.FeedService;

/**
 * This interface represents a {@link Listener} for {@link FeedService} events
 * in respect of {@link Attachment}s.
 * 
 * @author Tobias Schlecht
 */
public interface AttachmentListener extends Listener
{
	/**
	 * Called when a new {@link Attachment} has been added. <br/><br/>
	 * 
	 * <i>Annotation:</i> Usually the {@link AttachmentID}s will be delivered in
	 * a chronological order. This means that {@code AttachmentID}s of earlier
	 * published {@code Attachment}s will appear before {@code AttachmentID}s of
	 * later published {@code Attachment}s. But due to concurrency there is
	 * <u>no guarantee</u> that this will always happen in the described manner.
	 * 
	 * @param attachmentID The {@code AttachmentID} of the new
	 *        {@code Attachment}
	 */
	void onAttachmentAdded(AttachmentID attachmentID);
}
