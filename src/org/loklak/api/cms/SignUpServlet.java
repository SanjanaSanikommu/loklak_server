/**
 *  SignUpServlet
 *  Copyright 27.05.2015 by Robert Mader, @treba13
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.api.cms;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.APIServiceLevel;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authentication;
import org.loklak.server.Authorization;
import org.loklak.server.ClientCredential;
import org.loklak.server.ClientIdentity;
import org.loklak.server.Query;

public class SignUpServlet extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    public APIServiceLevel getDefaultServiceLevel() {
        return APIServiceLevel.PUBLIC;
    }

    @Override
    public APIServiceLevel getCustomServiceLevel(Authorization rights) {
        return APIServiceLevel.ADMIN;
    }

    public String getAPIPath() {
        return "/api/signup.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, Authorization rights) throws APIException {

    	JSONObject result = new JSONObject();
    	
    	boolean activated = true;
    	boolean sendEmail = false;
    	
    	if(!rights.isAdmin()){
    		switch(DAO.getConfig("users.public.signup", "false")){
    			case "false":
    				result.put("status", "error");
    	    		result.put("reason", "Public signup disabled");
    	    		return result;
    			case "admin":
    				activated = false;
    				break;
    			case "email":
    				activated = false;
    				sendEmail = true;
    		}
    	}
    	
    	if(post.get("signup",null) == null || post.get("password", null) == null){
    		result.put("status", "error");
    		result.put("reason", "signup or password empty");
    		return result;
    	}
    	
    	String signup, password;
    	try {
    		signup = URLDecoder.decode(post.get("signup",null),"UTF-8");
			password = URLDecoder.decode(post.get("password",null),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			result.put("status", "error");
    		result.put("reason", "malformed query");
    		return result;
		}
    	
    	
    	ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, signup);
    	Authentication authentication = new Authentication(credential, DAO.authentication);
    	
    	if (authentication.getIdentity() != null) {
    		result.put("status", "error");
    		result.put("reason", "email already taken");
    		return result;
    	}
    	
    	ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, credential.getName());
    	authentication.setIdentity(identity);
    	
    	String salt = createRandomString(20);
    	authentication.put("salt", salt);
    	authentication.put("passwordHash", getHash(password, salt));
    	authentication.put("activated", activated);
        
        if(sendEmail){
	        String token = createRandomString(30);
	        ClientCredential loginToken = new ClientCredential(ClientCredential.Type.login_token, token);
	        Authentication tokenAuthentication = new Authentication(loginToken, DAO.authentication);
	        tokenAuthentication.setIdentity(identity); // persistency is wierd
	        tokenAuthentication.setExpireTime(7 * 24 * 60 * 60);
	        
	        // TODO: send email
        }

    	result.put("status", "ok");
		result.put("reason", "ok");
		return result;
    }
    
}
