package mealyummy.mealservice.service.iam.token;

import java.util.List;

public interface TokenService {
    void storeToken(String username, String token, String prefix, long expiration);
    boolean isAccessTokenValid(String username, String token);
    boolean isRefreshTokenValid(String username, String token);
    void removeAllTokensOfUser(String username);
    List<String> getActiveUsers();
}
