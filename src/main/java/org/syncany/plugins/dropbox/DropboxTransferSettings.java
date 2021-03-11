/*
 * Syncany, www.syncany.org
 * Copyright (C) 2011-2014 Philipp C. Heckel <philipp.heckel@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.syncany.plugins.dropbox;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dropbox.core.DbxAuthFinish;
import org.simpleframework.xml.Element;
import org.syncany.plugins.dropbox.DropboxTransferSettings.DropboxOAuthGenerator;
import org.syncany.plugins.transfer.Encrypted;
import org.syncany.plugins.transfer.Setup;
import org.syncany.plugins.transfer.StorageException;
import org.syncany.plugins.transfer.TransferSettings;
import org.syncany.plugins.transfer.oauth.OAuth;
import org.syncany.plugins.transfer.oauth.OAuthGenerator;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.DbxSessionStore;
import com.dropbox.core.DbxWebAuth;
import com.google.common.collect.Maps;

/**
 * @author Christian Roth <christian.roth@port17.de>
 */
@OAuth(value = DropboxOAuthGenerator.class, callbackId = "dropbox", callbackPort = 6462)
public class DropboxTransferSettings extends TransferSettings {
	private static final Logger logger = Logger.getLogger(DropboxTransferSettings.class.getName());

	private DbxWebAuth webAuth;
	private DbxWebAuth webAuthNoRedirect;
	private DbxSessionStore csrfTokenStore;

	@Element(name = "accessToken", required = true)
	@Setup(order = 1, visible = false)
	@Encrypted
	public String accessToken;

	@Element(name = "path", required = true)
	@Setup(order = 2, description = "Relative path on Dropbox")
	public String path;

	public String getAccessToken() {
		return accessToken;
	}

	public String getPath() {
		return path;
	}

	public class DropboxOAuthGenerator implements OAuthGenerator, OAuthGenerator.WithNoRedirectMode {
		@Override
		public URI generateAuthUrl(URI redirectUri) throws StorageException {
			logger.log(Level.INFO, "Operation mode is redirect_url");

			csrfTokenStore = new DbxSessionStore() {
				private String token;

				@Override
				public String get() {
					return token;
				}

				@Override
				public void set(String value) {
					this.token = value;
				}

				@Override
				public void clear() {
					token = null;
				}
			};

			webAuth = new DbxWebAuth(DropboxTransferPlugin.DROPBOX_REQ_CONFIG, DropboxTransferPlugin.DROPBOX_APP_INFO);
			DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
					.withRedirectUri(redirectUri.toString(), csrfTokenStore)
					.build();

			try {
				return new URI(webAuth.authorize(authRequest));
			}
			catch (URISyntaxException e) {
				throw new StorageException(e);
			}
		}

		@Override
		public URI generateAuthUrl() throws StorageException {
			logger.log(Level.INFO, "Operation mode is no_redirect_uri");

			webAuthNoRedirect = new DbxWebAuth(DropboxTransferPlugin.DROPBOX_REQ_CONFIG,DropboxTransferPlugin.DROPBOX_APP_INFO);
			DbxWebAuth.Request authRequest = DbxWebAuth.newRequestBuilder()
					.withNoRedirect().build();

			try {
				return new URI(webAuthNoRedirect.authorize(authRequest));
			}
			catch (URISyntaxException e) {
				throw new StorageException(e);
			}
		}

		@Override
		public void checkToken(String code, String csrfState) throws StorageException {
			DbxAuthFinish authFinish;

			try {
				if (webAuth != null) {
					logger.log(Level.INFO, "Extracting token from redirect_url");

					Map<String, String[]> responseMap = Maps.newHashMap();
					responseMap.put("code", new String[]{code});
					responseMap.put("state", new String[]{csrfState});

					authFinish = webAuth.finishFromRedirect("http://localhost:6462/dropbox/",csrfTokenStore,responseMap);
					accessToken = authFinish.getAccessToken();

				}
				else if (webAuthNoRedirect != null) {
					logger.log(Level.INFO, "Extracting token from copy-paste code");
					authFinish = webAuthNoRedirect.finishFromCode(code);
					accessToken = authFinish.getAccessToken();
				}

				DbxClientV2 client = new DbxClientV2(DropboxTransferPlugin.DROPBOX_REQ_CONFIG, accessToken);

				client.users().getCurrentAccount(); // Throws exception if this fails!
			}
			catch (Exception e) {
				throw new RuntimeException("Error requesting dropbox data: " + e.getMessage());
			}
		}
	}
}
