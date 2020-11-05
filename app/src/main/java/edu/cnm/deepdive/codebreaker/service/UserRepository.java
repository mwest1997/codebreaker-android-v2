package edu.cnm.deepdive.codebreaker.service;

import android.content.Context;
import edu.cnm.deepdive.codebreaker.model.dao.UserDao;
import edu.cnm.deepdive.codebreaker.model.entity.User;
import io.reactivex.Single;

public class UserRepository {

  private final Context context;
  private final UserDao userDao;
  private final CodebreakerWebService webService;
  private final GoogleSignInService signInService;

  public UserRepository(Context context) {
    this.context = context;
    userDao = CodebreakerDatabase.getInstance().getUserDao();
    webService = CodebreakerWebService.getInstance();
    signInService = GoogleSignInService.getInstance();
  }

  public Single<User> getCurrentUser() {
    return Single.fromCallable(signInService::getAccount)
        .flatMap((account) ->
            webService.getProfile(getBearerToken(account.getIdToken()))
                .flatMap((user) ->
                    userDao.selectByOauthKey(account.getId())
                        .switchIfEmpty(
                            userDao.insert(user)
                                .map((id) -> {
                                  user.setId(id);
                                  return user;
                                })
                        )
                )
            .flatMap((user) ->
                userDao.update(user)
                    .map((numRecords) -> user)
            )
        );

  }

  private String getBearerToken(String idToken) {
    return String.format("Bearer %s", idToken);
  }

}
