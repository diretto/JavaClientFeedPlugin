package org.diretto.api.client.main.feed;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.configuration.XMLConfiguration;
import org.diretto.api.client.JavaClient;
import org.diretto.api.client.JavaClientImpl;
import org.diretto.api.client.base.annotations.InvocationLimited;
import org.diretto.api.client.base.entities.Entity;
import org.diretto.api.client.base.entities.EntityID;
import org.diretto.api.client.base.types.LoadType;
import org.diretto.api.client.main.core.CoreService;
import org.diretto.api.client.main.core.entities.Attachment;
import org.diretto.api.client.main.core.entities.AttachmentID;
import org.diretto.api.client.main.core.entities.Comment;
import org.diretto.api.client.main.core.entities.CommentID;
import org.diretto.api.client.main.core.entities.CoreServiceEntityIDFactory;
import org.diretto.api.client.main.core.entities.Document;
import org.diretto.api.client.main.core.entities.DocumentID;
import org.diretto.api.client.main.feed.binding.FeedServiceInstanceDataResource;
import org.diretto.api.client.main.feed.event.AttachmentListener;
import org.diretto.api.client.main.feed.event.CommentListener;
import org.diretto.api.client.main.feed.event.DocumentListener;
import org.diretto.api.client.main.feed.event.Listener;
import org.diretto.api.client.main.feed.subscriber.FeedHandler;
import org.diretto.api.client.main.feed.subscriber.Subscriber;
import org.diretto.api.client.service.AbstractService;
import org.diretto.api.client.util.InvocationUtils;
import org.diretto.api.client.util.URLTransformationUtils;
import org.joda.time.DateTime;
import org.restlet.Client;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.ext.atom.Link;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;

import com.google.pubsubhubbub.java.subscriber.Discovery;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Feed;

/**
 * This class is the implementation class of the {@link FeedService} interface.
 * <br/><br/>
 * 
 * <i>Annotation:</i> The {@code PubSubHubbub} protocol is used for the
 * implementation.
 * 
 * @author Tobias Schlecht
 */
public final class FeedServiceImpl extends AbstractService implements FeedService, Comparator<Entry>
{
	private final CoreService coreService;
	private final boolean cacheActivated;
	private final XMLConfiguration xmlConfiguration;
	private final boolean preloadNewDocuments;
	private final boolean hubFailureFallbackActivated;

	private Client restletClient;
	private int paginationSize;

	private final List<DocumentListener> documentListeners = new Vector<DocumentListener>();
	private final List<AttachmentListener> attachmentListeners = new Vector<AttachmentListener>();
	private final List<CommentListener> commentListeners = new Vector<CommentListener>();

	private DocumentID latestDocumentID = null;
	private AttachmentID latestAttachmentID = null;
	private CommentID latestCommentID = null;

	private final DateTime initTime;

	private final Map<String, String> feedURLs = new HashMap<String, String>();

	/**
	 * The constructor is {@code private} to have strict control what instances
	 * exist at any time. Instead of the constructor the {@code public}
	 * <i>static factory method</i> {@link #getInstance(URL, JavaClient)}
	 * returns the instances of the class.
	 * 
	 * @param serviceURL The service {@code URL}
	 * @param javaClient The corresponding {@code JavaClient}
	 */
	private FeedServiceImpl(URL serviceURL, JavaClient javaClient)
	{
		super(FeedServiceID.INSTANCE, serviceURL, javaClient);

		initTime = new DateTime();

		coreService = javaClient.getCoreService();
		cacheActivated = coreService.isCacheActivated();

		xmlConfiguration = FeedServiceID.INSTANCE.getXMLConfiguration();

		preloadNewDocuments = xmlConfiguration.getBoolean("core-service-cache/preload-new-documents");
		hubFailureFallbackActivated = xmlConfiguration.getBoolean("pubsubhubbub/hub-failure-fallback");

		if(hubFailureFallbackActivated)
		{
			restletClient = ((JavaClientImpl) javaClient).getRestletClient();

			ClientResource clientResource = new ClientResource(serviceURL.toExternalForm());
			clientResource.setNext(restletClient);
			FeedServiceInstanceDataResource feedServiceInstanceDataResource = clientResource.get(FeedServiceInstanceDataResource.class);

			System.out.println("[FeedService FeedServiceImpl] " + serviceURL.toExternalForm());

			paginationSize = feedServiceInstanceDataResource.getParameters().getPaginationSize();
		}

		FeedHandler feedHandler = new FeedHandler()
		{
			@Override
			public void onFeedUpdate(String feedURL, Feed feed)
			{
				@SuppressWarnings("unchecked")
				List<Entry> entries = feed.getEntries();

				if(hubFailureFallbackActivated && entries.size() < paginationSize)
				{
					sortEntries(entries);
				}

				if(feedURL.equals(feedURLs.get("documentFeed")))
				{
					handleDocumentFeedUpdate(entries);
				}
				else if(feedURL.equals(feedURLs.get("attachmentFeed")))
				{
					handleAttachmentFeedUpdate(entries);
				}
				else if(feedURL.equals(feedURLs.get("commentFeed")))
				{
					handleCommentFeedUpdate(entries);
				}
			}
		};

		Subscriber subscriber = Subscriber.getInstance(xmlConfiguration, feedHandler);

		String[] names = xmlConfiguration.getStringArray("feeds/feed/@name");
		String[] urls = xmlConfiguration.getStringArray("feeds/feed/@url");

		Discovery discovery = new Discovery();

		String feedURL = null;

		for(int i = 0; i < names.length; i++)
		{
			feedURL = serviceURL.toExternalForm() + urls[i];

			feedURLs.put(names[i], feedURL);

			try
			{
				subscriber.subscribe(discovery.getHub(feedURL), feedURL);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns a {@link FeedService} instance for the specified service
	 * {@link URL} and the corresponding {@link JavaClient}.
	 * 
	 * @param serviceURL The service {@code URL}
	 * @param javaClient The corresponding {@code JavaClient}
	 * @return A {@code FeedService} instance
	 */
	@InvocationLimited(legitimateInvocationClasses = {JavaClientImpl.class})
	public static synchronized FeedService getInstance(URL serviceURL, JavaClient javaClient)
	{
		serviceURL = URLTransformationUtils.adjustServiceURL(serviceURL);

		String warningMessage = "The method invocation \"" + FeedServiceImpl.class.getCanonicalName() + ".getInstance(URL, JavaClient)\" is not intended for this usage. Use the method \"" + JavaClient.class.getCanonicalName() + ".getService(ServicePluginID)\" instead.";
		InvocationUtils.checkMethodInvocation(warningMessage, "getInstance", URL.class, JavaClient.class);

		return new FeedServiceImpl(serviceURL, javaClient);
	}

	@Override
	public synchronized void addDocumentListener(DocumentListener documentListener)
	{
		if(!documentListeners.contains(documentListener))
		{
			documentListeners.add(documentListener);
		}
	}

	@Override
	public synchronized void addAttachmentListener(AttachmentListener attachmentListener)
	{
		if(!attachmentListeners.contains(attachmentListener))
		{
			attachmentListeners.add(attachmentListener);
		}
	}

	@Override
	public synchronized void addCommentListener(CommentListener commentListener)
	{
		if(!commentListeners.contains(commentListener))
		{
			commentListeners.add(commentListener);
		}
	}

	@Override
	public synchronized void removeListener(Listener listener)
	{
		if(listener instanceof DocumentListener)
		{
			documentListeners.remove(listener);
		}
		else if(listener instanceof AttachmentListener)
		{
			attachmentListeners.remove(listener);
		}
		else if(listener instanceof CommentListener)
		{
			commentListeners.remove(listener);
		}
	}

	/**
	 * Handles a {@link Feed} update of the {@link Document} {@code Feed}.
	 * 
	 * @param entries A {@code List} with the {@code Atom} {@code Feed} entries
	 */
	private synchronized void handleDocumentFeedUpdate(List<Entry> entries)
	{
		final List<DocumentID> documentIDs = new Vector<DocumentID>();

		if(!hubFailureFallbackActivated || entries.size() < paginationSize)
		{
			for(int i = entries.size() - 1; i >= 0; i--)
			{
				final DocumentID documentID = CoreServiceEntityIDFactory.getDocumentIDInstance(entries.get(i).getId());

				documentIDs.add(documentID);

				latestDocumentID = documentID;

				for(final DocumentListener documentListener : documentListeners)
				{
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							documentListener.onDocumentAdded(documentID);
						}
					}).start();
				}
			}
		}
		else
		{
			String latestDocumentIDString = null;

			if(latestDocumentID != null)
			{
				latestDocumentIDString = latestDocumentID.toString();
			}

			List<String> documentIDStrings = getHubFailureFallbackEntryIDs(feedURLs.get("documentFeed"), latestDocumentIDString);

			for(int i = documentIDStrings.size() - 1; i >= 0; i--)
			{
				final DocumentID documentID = CoreServiceEntityIDFactory.getDocumentIDInstance(documentIDStrings.get(i));

				documentIDs.add(documentID);

				latestDocumentID = documentID;

				for(final DocumentListener documentListener : documentListeners)
				{
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							documentListener.onDocumentAdded(documentID);
						}
					}).start();
				}
			}
		}

		if(preloadNewDocuments && cacheActivated && documentIDs.size() > 0)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					coreService.getDocumentsByIDs(documentIDs, LoadType.COMPLETE, false);
				}
			}).start();
		}
	}

	/**
	 * Handles a {@link Feed} update of the {@link Attachment} {@code Feed}.
	 * 
	 * @param entries A {@code List} with the {@code Atom} {@code Feed} entries
	 */
	private synchronized void handleAttachmentFeedUpdate(List<Entry> entries)
	{
		if(!hubFailureFallbackActivated || entries.size() < paginationSize)
		{
			for(int i = entries.size() - 1; i >= 0; i--)
			{
				String attachmentIDString = entries.get(i).getId();

				String documentIDString = URLTransformationUtils.removeSubEntityPart(attachmentIDString);

				DocumentID documentID = CoreServiceEntityIDFactory.getDocumentIDInstance(documentIDString);

				final AttachmentID attachmentID = CoreServiceEntityIDFactory.getAttachmentIDInstance(attachmentIDString, documentID, documentID);

				latestAttachmentID = attachmentID;

				for(final AttachmentListener attachmentListener : attachmentListeners)
				{
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							attachmentListener.onAttachmentAdded(attachmentID);
						}
					}).start();
				}
			}
		}
		else
		{
			String latestAttachmentIDString = null;

			if(latestAttachmentID != null)
			{
				latestAttachmentIDString = latestAttachmentID.toString();
			}

			List<String> attachmentIDStrings = getHubFailureFallbackEntryIDs(feedURLs.get("attachmentFeed"), latestAttachmentIDString);

			for(int i = attachmentIDStrings.size() - 1; i >= 0; i--)
			{
				String attachmentIDString = attachmentIDStrings.get(i);

				String documentIDString = URLTransformationUtils.removeSubEntityPart(attachmentIDString);

				DocumentID documentID = CoreServiceEntityIDFactory.getDocumentIDInstance(documentIDString);

				final AttachmentID attachmentID = CoreServiceEntityIDFactory.getAttachmentIDInstance(attachmentIDString, documentID, documentID);

				latestAttachmentID = attachmentID;

				for(final AttachmentListener attachmentListener : attachmentListeners)
				{
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							attachmentListener.onAttachmentAdded(attachmentID);
						}
					}).start();
				}
			}
		}
	}

	/**
	 * Handles a {@link Feed} update of the {@link Comment} {@code Feed}.
	 * 
	 * @param entries A {@code List} with the {@code Atom} {@code Feed} entries
	 */
	private synchronized void handleCommentFeedUpdate(List<Entry> entries)
	{
		if(!hubFailureFallbackActivated || entries.size() < paginationSize)
		{
			for(int i = entries.size() - 1; i >= 0; i--)
			{
				String commentIDString = entries.get(i).getId();

				String documentIDString = URLTransformationUtils.removeSubEntityPart(commentIDString);

				DocumentID documentID = CoreServiceEntityIDFactory.getDocumentIDInstance(documentIDString);

				final CommentID commentID = CoreServiceEntityIDFactory.getCommentIDInstance(commentIDString, documentID, documentID);

				latestCommentID = commentID;

				for(final CommentListener commentListener : commentListeners)
				{
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							commentListener.onCommentAdded(commentID);
						}
					}).start();
				}
			}
		}
		else
		{
			String latestCommentIDString = null;

			if(latestCommentID != null)
			{
				latestCommentIDString = latestCommentID.toString();
			}

			List<String> commentIDStrings = getHubFailureFallbackEntryIDs(feedURLs.get("commentFeed"), latestCommentIDString);

			for(int i = commentIDStrings.size() - 1; i >= 0; i--)
			{
				String commentIDString = commentIDStrings.get(i);

				String documentIDString = URLTransformationUtils.removeSubEntityPart(commentIDString);

				DocumentID documentID = CoreServiceEntityIDFactory.getDocumentIDInstance(documentIDString);

				final CommentID commentID = CoreServiceEntityIDFactory.getCommentIDInstance(commentIDString, documentID, documentID);

				latestCommentID = commentID;

				for(final CommentListener commentListener : commentListeners)
				{
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							commentListener.onCommentAdded(commentID);
						}
					}).start();
				}
			}
		}
	}

	/**
	 * This method serves as fallback in the case that a hub fails (e.g. if the
	 * hub is not able to deliver the new contents fast enough and therefore the
	 * number of entries which should be updated exceeds the number of entries
	 * which are displayed on one {@code Feed} page). Hence this method offers
	 * the possibility to receive the {@link EntityID}s in {@code String}
	 * representation of all {@link Entity}s which are updated after the
	 * {@code Entity} corresponding to the given {@link EntityID}.
	 * 
	 * @param feedURL The {@code URL} ({@code String} representation) of the
	 *        corresponding {@code Feed}
	 * @param entityID The {@code EntityID} ({@code String} representation)
	 *        after which the entries should be returned
	 * @return A {@code Stack} with the requested {@code EntityID}s in
	 *         {@code String} representation
	 */
	private Stack<String> getHubFailureFallbackEntryIDs(String feedURL, String entityID)
	{
		return getHubFailureFallbackEntryIDs(feedURL, entityID, new Stack<String>());
	}

	/**
	 * This method serves as fallback in the case that a hub fails (e.g. if the
	 * hub is not able to deliver the new contents fast enough and therefore the
	 * number of entries which should be updated exceeds the number of entries
	 * which are displayed on one {@code Feed} page). Hence this method offers
	 * the possibility to receive the {@link EntityID}s in {@code String}
	 * representation of all {@link Entity}s which are updated after the
	 * {@code Entity} corresponding to the given {@link EntityID}.
	 * 
	 * @param feedURL The {@code URL} ({@code String} representation) of the
	 *        corresponding {@code Feed}
	 * @param entityID The {@code EntityID} ({@code String} representation)
	 *        after which the entries should be returned
	 * @param idStrings This parameter serves as recursion parameter and should
	 *        be initially supplied with an empty {@code String} {@code Stack}
	 * @return A {@code Stack} with the requested {@code EntityID}s in
	 *         {@code String} representation
	 */
	private Stack<String> getHubFailureFallbackEntryIDs(String feedURL, String entityID, Stack<String> idStrings)
	{
		ClientResource clientResource = new ClientResource(feedURL);
		clientResource.setNext(restletClient);
		Representation representation = clientResource.get(MediaType.APPLICATION_ATOM);

		try
		{
			org.restlet.ext.atom.Feed feed = new org.restlet.ext.atom.Feed(representation);

			List<org.restlet.ext.atom.Entry> entries = feed.getEntries();

			for(org.restlet.ext.atom.Entry entry : entries)
			{
				if(!entry.getId().equals(entityID) && entry.getUpdated().after(initTime.toDate()))
				{
					idStrings.push(entry.getId());
				}
				else
				{
					return idStrings;
				}
			}

			Reference reference = null;

			List<Link> links = feed.getLinks();

			for(Link link : links)
			{
				if(link.getRel().getName().equals("previous"))
				{
					reference = link.getHref();

					break;
				}
			}

			if(reference != null)
			{
				return getHubFailureFallbackEntryIDs(reference.toString(), entityID, idStrings);
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return idStrings;
	}

	/**
	 * Sorts the given {@code List} of {@link Entry}s by their update
	 * {@link Date}. <br/><br/>
	 * 
	 * <i/>Annotation:</i> The result is a {@code List} which starts with the
	 * latest updated {@code Entry}s and ends with the oldest updated
	 * {@code Entry}s.
	 * 
	 * @param entries The {@code List} of {@code Entry}s to sort
	 */
	private void sortEntries(List<Entry> entries)
	{
		Collections.sort(entries, this);
	}

	@Override
	public int compare(Entry entry1, Entry entry2)
	{
		if(entry1.getUpdated().equals(entry2.getUpdated()))
		{
			return 0;
		}
		else if(entry1.getUpdated().before(entry2.getUpdated()))
		{
			return 1;
		}

		return -1;
	}
}
