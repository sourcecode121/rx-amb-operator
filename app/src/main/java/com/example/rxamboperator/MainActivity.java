package com.example.rxamboperator;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.rxamboperator.models.Customer;

import java.util.Random;
import java.util.UUID;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private Subscription sub;
    private Thread firstThread;
    private Thread secondThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);
        final TextView textView = (TextView) findViewById(R.id.textView);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                killThreads();

                sub = Observable.amb(getCustomerFromServerOne(), getCustomerFromServerTwo())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Action0() {
                            @Override
                            public void call() {
                                view.setEnabled(false);
                            }
                        })
                        .subscribe(new Subscriber<Customer>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.d("Amb", e.getMessage(), e);
                            }

                            @Override
                            public void onNext(Customer customer) {
                                Log.d("Amb", customer.getName());
                                textView.setText(textView.getText() + "\n" + customer.getName());
                                unsubscribe();
                                view.setEnabled(true);
                            }
                        });
            }
        });
    }

    public Observable<Customer> getCustomerFromServerOne(){
        return getCustomerOne();
    }

    public Observable<Customer> getCustomerFromServerTwo(){
        return getCustomerTwo();
    }

    @NonNull
    private Observable<Customer> getCustomerOne(){
        return Observable.defer(new Func0<Observable<Customer>>() {
            @Override
            public Observable<Customer> call() {
                return Observable.create(new Observable.OnSubscribe<Customer>() {
                    @Override
                    public void call(final Subscriber<? super Customer> subscriber) {
                        if(subscriber != null && !subscriber.isUnsubscribed()){
                            firstThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(getRandomTime());
                                        subscriber.onNext(new Customer("Customer one", UUID.randomUUID().toString()));
                                        subscriber.onCompleted();
                                    }
                                    catch (InterruptedException e){
                                        subscriber.onError(e);
                                    }
                                }
                            });
                            firstThread.start();
                        }
                    }
                });
            }
        });
    }

    @NonNull
    private Observable<Customer> getCustomerTwo(){
        return Observable.defer(new Func0<Observable<Customer>>() {
            @Override
            public Observable<Customer> call() {
                return Observable.create(new Observable.OnSubscribe<Customer>() {
                    @Override
                    public void call(final Subscriber<? super Customer> subscriber) {
                        if(subscriber != null && !subscriber.isUnsubscribed()){
                            secondThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(getRandomTime());
                                        subscriber.onNext(new Customer("Customer two", UUID.randomUUID().toString()));
                                        subscriber.onCompleted();
                                    }
                                    catch (InterruptedException e){
                                        subscriber.onError(e);
                                    }
                                }
                            });
                            secondThread.start();
                        }
                    }
                });
            }
        });
    }

    public long getRandomTime(){
        Random ran = new Random();
        long x = ran.nextInt(500) + 1000;
        Log.d("Amb", "Random time: " + x);
        return x;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        killThreads();
        if(sub != null && !sub.isUnsubscribed()){
            sub.unsubscribe();
        }
    }

    private void killThreads(){
        if(firstThread != null){
            firstThread.interrupt();
        }
        if(secondThread != null){
            secondThread.interrupt();
        }
    }
}
