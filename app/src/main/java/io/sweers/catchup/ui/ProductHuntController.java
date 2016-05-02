package io.sweers.catchup.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.Lazy;
import dagger.Provides;
import io.sweers.catchup.BuildConfig;
import io.sweers.catchup.R;
import io.sweers.catchup.data.AuthInterceptor;
import io.sweers.catchup.data.producthunt.ProductHuntService;
import io.sweers.catchup.data.producthunt.model.Post;
import io.sweers.catchup.data.producthunt.model.PostsResponse;
import io.sweers.catchup.injection.API;
import io.sweers.catchup.injection.PerController;
import io.sweers.catchup.ui.activity.ActivityComponent;
import io.sweers.catchup.ui.activity.MainActivity;
import io.sweers.catchup.ui.base.BasicNewsController;
import io.sweers.catchup.util.customtabs.CustomTabActivityHelper;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import rx.Observable;
import timber.log.Timber;

import static io.sweers.catchup.data.producthunt.ProductHuntService.DATE_FORMAT;


public final class ProductHuntController extends BasicNewsController<Post> {

  @Inject ProductHuntService service;
  @Inject CustomTabActivityHelper customTab;

  public ProductHuntController() {
    this(null);
  }

  public ProductHuntController(Bundle args) {
    super(args);
  }

  @Override protected void performInjection() {
    DaggerProductHuntController_Component
        .builder()
        .module(new Module())
        .activityComponent(((MainActivity) getActivity()).getComponent())
        .build()
        .inject(this);
  }

  @Override protected Context onThemeContext(@NonNull Context context) {
    return new ContextThemeWrapper(context, R.style.CatchUp_ProductHunt);
  }

  @Override
  protected void bindItemView(@NonNull ViewHolder holder, @NonNull View view, @NonNull Post item) {
    holder.title(item.name());
    holder.score(String.format(Locale.getDefault(), "▲ %d", item.votes_count()));
    try {
      holder.timestamp(DATE_FORMAT.parse(item.created_at()).getTime() / 1000);
    } catch (ParseException e) {
      Timber.e(e, "Parsing date failed");
    }
    holder.author(item.user().name());
    holder.source(item.getFirstTopic());
    holder.comments(item.comments_count());
  }

  @Override
  protected void onItemClick(@NonNull ViewHolder holder, @NonNull View view, @NonNull Post item) {
    // TODO Make the app choice a pref
    customTab.openCustomTab(customTab.getCustomTabIntent()
            .setToolbarColor(getServiceThemeColor())
            .build(),
        Uri.parse(item.redirect_url()));
  }

  @Override
  protected void onCommentClick(@NonNull ViewHolder holder, @NonNull View view, @NonNull Post item) {
    // TODO Make the app choice a pref
    customTab.openCustomTab(customTab.getCustomTabIntent()
            .setToolbarColor(getServiceThemeColor())
            .build(),
        Uri.parse(item.discussion_url()));
  }

  @NonNull @Override protected Observable<List<Post>> getDataObservable() {
    return service.getPosts(0)
        .map(PostsResponse::posts);

  }

  @PerController
  @dagger.Component(
      modules = Module.class,
      dependencies = ActivityComponent.class
  )
  public interface Component {
    void inject(ProductHuntController controller);
  }

  @dagger.Module
  public static class Module {

    @Provides
    @PerController
    @API
    OkHttpClient provideProductHuntOkHttpClient(OkHttpClient client) {
      return client
          .newBuilder()
          .addInterceptor(new AuthInterceptor(BuildConfig.PROCUCT_HUNT_DEVELOPER_TOKEN))
          .build();
    }

    @Provides
    @PerController
    ProductHuntService provideProductHuntService(
        @API final Lazy<OkHttpClient> client,
        MoshiConverterFactory moshiConverterFactory,
        RxJavaCallAdapterFactory rxJavaCallAdapterFactory) {
      return new Retrofit.Builder()
          .baseUrl(ProductHuntService.ENDPOINT)
          .callFactory(request -> client.get().newCall(request))
          .addCallAdapterFactory(rxJavaCallAdapterFactory)
          .addConverterFactory(moshiConverterFactory)
          .build()
          .create(ProductHuntService.class);
    }
  }
}