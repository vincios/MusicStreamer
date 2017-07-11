package com.vincios.musicstreamer2.connectors.pleer;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PleerConstants {

    //EXCEPTION HANDLING

    public static final String RESPONSE_JSON_ERROR = "error";
    public static final String RESPONSE_JSON_ERROR_DESCRIPTION = "error_description";
    public static final String RESPONSE_JSON_INSUCCESS_MESSAGE = "message";

    //METHOD TYPES
    public static final String METHOD_FIND = "tracks_search";
    public static final String METHOD_GET_DOWNLOAD_LINK = "tracks_get_download_link";
    public static final String METHOD_GET_TOKEN = "get_token";

    //CONNECTION URLS
    public static final String TOKEN_URL = "http://api.pleer.com/token.php";
    public static final String REQUEST_URL = "http://api.pleer.com/index.php";


    //PARAMS TOKEN REQUEST
    public static final String POST_KEY_RESPONSE_TYPE = "response_type";
    public static final String POST_KEY_CLIENT_ID = "client_id";
    public static final String POST_KEY_GRANT_TYPE = "grant_type";
    public static final String POST_VALUE_RESPONSE_TYPE = "code";
    public static final String POST_VALUE_CLIENT_ID = "30719KSrFyi2XCdY4q2a";
    public static final String POST_VALUE_GRANT_TYPE = "client_credentials";

    //PARAMS REQUESTS
    public static final String POST_KEY_ACCESS_TOKEN = "access_token";
    public static final String POST_KEY_METHOD = "method";

    public static final String POST_KEY_TRACK_ID = "track_id";
    public static final String POST_KEY_REASON = "reason";
    public static final String POST_VALUE_REASON = "listen";

    public static final String POST_KEY_QUERY = "query";


    public static final Map<String, String> tokenRequestParams;

    static {
        Map<String, String> map = new HashMap<>(4);
        map.put(PleerConstants.POST_KEY_METHOD, PleerConstants.METHOD_GET_TOKEN);
        map.put(PleerConstants.POST_KEY_RESPONSE_TYPE, PleerConstants.POST_VALUE_RESPONSE_TYPE);
        map.put(PleerConstants.POST_KEY_CLIENT_ID, PleerConstants.POST_VALUE_CLIENT_ID);
        map.put(PleerConstants.POST_KEY_GRANT_TYPE, PleerConstants.POST_VALUE_GRANT_TYPE);

        tokenRequestParams = Collections.unmodifiableMap(map);
    }


}
