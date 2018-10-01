package com.moselo.HomingPigeon.API.View;

import com.moselo.HomingPigeon.Model.ErrorModel;

/**
 * Created by Fadhlan on 6/15/17.
 */

public abstract class DefaultDataView<T> implements IView<T> {
    @Override
    public void startLoading() {

    }

    @Override
    public void endLoading() {

    }

    @Override
    public void onEmpty(String message) {

    }

    @Override
    public void onSuccess(T t) {

    }

    @Override
    public void onSucessMessage(String message) {

    }

    @Override
    public void onError(ErrorModel error) {

    }

    @Override
    public void onError(String errorMessage) {

    }

    @Override
    public void onError(Throwable throwable) {

    }
}