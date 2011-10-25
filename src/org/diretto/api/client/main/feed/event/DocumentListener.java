package org.diretto.api.client.main.feed.event;

import org.diretto.api.client.main.core.entities.Document;
import org.diretto.api.client.main.core.entities.DocumentID;
import org.diretto.api.client.main.feed.FeedService;

/**
 * This interface represents a {@link Listener} for {@link FeedService} events
 * in respect of {@link Document}s.
 * 
 * @author Tobias Schlecht
 */
public interface DocumentListener extends Listener
{
	/**
	 * Called when a new {@link Document} has been added. <br/><br/>
	 * 
	 * <i>Annotation:</i> Usually the {@link DocumentID}s will be delivered in a
	 * chronological order. This means that {@code DocumentID}s of earlier
	 * published {@code Document}s will appear before {@code DocumentID}s of
	 * later published {@code Document}s. But due to concurrency there is <u>no
	 * guarantee</u> that this will always happen in the described manner.
	 * 
	 * @param documentID The {@code DocumentID} of the new {@code Document}
	 */
	void onDocumentAdded(DocumentID documentID);
}
