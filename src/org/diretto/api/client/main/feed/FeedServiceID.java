package org.diretto.api.client.main.feed;

import org.apache.commons.configuration.XMLConfiguration;
import org.diretto.api.client.service.AbstractServicePluginID;
import org.diretto.api.client.service.Service;
import org.diretto.api.client.util.ConfigUtils;

/**
 * This class serves for the identification of the {@link FeedService}.
 * <br/><br/>
 * 
 * <i>Annotation:</i> <u>Singleton Pattern</u>
 * 
 * @author Tobias Schlecht
 */
public final class FeedServiceID extends AbstractServicePluginID
{
	private static final String CONFIG_FILE = "org/diretto/api/client/main/feed/config.xml";

	private static final XMLConfiguration xmlConfiguration = ConfigUtils.getXMLConfiguration(CONFIG_FILE);

	public static final FeedServiceID INSTANCE = new FeedServiceID(xmlConfiguration.getString("name"), xmlConfiguration.getString("api-version"), getInitServiceClass());

	/**
	 * Constructs the sole instance of the {@link FeedServiceID}. <br/><br/>
	 * 
	 * <i>Annotation:</i> <u>Singleton Pattern</u>
	 */
	private FeedServiceID(String name, String apiVersion, Class<Service> serviceClass)
	{
		super(name, apiVersion, serviceClass);
	}

	/**
	 * Returns the implementation class of the {@link FeedService}, which is
	 * loaded from the XML configuration file.
	 * 
	 * @return The implementation class of the {@code FeedService}
	 */
	@SuppressWarnings("unchecked")
	private static Class<Service> getInitServiceClass()
	{
		try
		{
			return (Class<Service>) Class.forName(xmlConfiguration.getString("service-class"));
		}
		catch(ClassNotFoundException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns the {@link XMLConfiguration} object, which is loaded from the XML
	 * configuration file corresponding to the whole {@link FeedService}
	 * implementation.
	 * 
	 * @return The {@code XMLConfiguration} object
	 */
	XMLConfiguration getXMLConfiguration()
	{
		return xmlConfiguration;
	}
}
