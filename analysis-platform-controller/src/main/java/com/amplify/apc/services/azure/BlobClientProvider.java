package com.amplify.apc.services.azure;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;

/**
 * Manages the storage blob client
 */
public class BlobClientProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(BlobClientProvider.class);

	/**
	 * Validates the connection string and returns the storage blob client. The
	 * connection string must be in the Azure connection string format.
	 *
	 * @return The newly created CloudBlobClient object
	 *
	 * @throws RuntimeException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws IllegalArgumentException
	 * @throws InvalidKeyException
	 */
	public static CloudBlobClient getBlobClientReference(StorageAccount azureStorageAccount)
			throws RuntimeException, IOException, IllegalArgumentException, URISyntaxException, InvalidKeyException {

		String storageConnectionString = "DefaultEndpointsProtocol=https;" + "AccountName=" + azureStorageAccount.name() + ";"
				+ "AccountKey=" + azureStorageAccount.getKeys().get(0).value();

		CloudStorageAccount storageAccount;
		try {
			storageAccount = CloudStorageAccount.parse(storageConnectionString);
		} catch (IllegalArgumentException | URISyntaxException e) {
			LOGGER.error(
					"Connection string specifies an invalid URI [{}] - Please confirm it is in Azure connection string format",
					storageConnectionString);
			throw e;
		} catch (InvalidKeyException e) {
			LOGGER.error(
					"Connection string specifies an invalid KEY [{}] - Please confirm that the AccountName and AccountKey in the connection string are valid.",
					storageConnectionString);
			throw e;
		}

		return storageAccount.createCloudBlobClient();
	}

}
