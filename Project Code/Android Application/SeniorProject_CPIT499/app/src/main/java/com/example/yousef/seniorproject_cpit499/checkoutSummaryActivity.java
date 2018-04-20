package com.example.yousef.seniorproject_cpit499;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class checkoutSummaryActivity extends AppCompatActivity {

    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    private DocumentReference user_database = FirebaseFirestore.getInstance().collection("users").document(user.getUid());
    private CollectionReference allProductsDB = FirebaseFirestore.getInstance().collection("products");

    private List<products> productsList;
    private checkoutListAdapter checkoutListAdapter;
    private RecyclerView list;

    private TextView tv_country, tv_city, tv_address, tv_orderTotal;
    private String country, city, address;
    private double orderTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_summary);

        initializeVaribles();
        getAddress();

        productsList = new ArrayList<>();
        checkoutListAdapter = new checkoutListAdapter(this, productsList);

        list = (RecyclerView) findViewById(R.id.summaryRecyclerView);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(checkoutListAdapter);
        retrieveData();

    }

    public void changeAddress(View view) {
        startActivity(new Intent(getApplicationContext(), addressActivity.class));
    }

    int newQuantity, count;

    //move item from cart and add it to order products list and subtract products quantity
    public void checkout(View view) {
        final String orderID = user_database.collection("user_orders").document().getId();
        //move item from this database
        user_database.collection("user_cart").addSnapshotListener(this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                for (final DocumentSnapshot doc : queryDocumentSnapshots) {
//Toast.makeText(checkoutSummaryActivity.this, "jj" + doc.getData(), Toast.LENGTH_SHORT).show();

                    Log.d("success", doc.getData() + " ... added to user order");

                    //Subtract product quantity from products database
                    Log.d("success", doc.getId() + "");


                    //then move item to this database
                    user_database.collection("user_orders").document(orderID).collection("purchases").document(doc.getId()).set(doc.getData()).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            /*allProductsDB.document(doc.getId()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                @Override
                                public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                                    Log.d("allProductsDB", documentSnapshot.getId() + "");
                                    Log.d("Products DB", documentSnapshot.getData() + "");
                                    int cartQuantity = doc.getLong("quantity").intValue();
                                    Log.d("cart Quantity", cartQuantity + "");
                                    int oldQuantity = documentSnapshot.getLong("quantity").intValue();
                                    Log.d("old Quantity", oldQuantity + "");
                                    newQuantity = oldQuantity - cartQuantity;
                                    Log.d("new Quantity", oldQuantity + "");
                                    allProductsDB.document(doc.getId()).update("quantity", newQuantity);
                                }
                            });*/
                        }
                    });

                    //delete data from cart after move it to order list
                    user_database.collection("user_cart").document(doc.getId()).delete();

                }
                Map<Object, Object> orderDetails = new HashMap<>();
                orderDetails.put("total", orderTotal);
                orderDetails.put("status", "In Process");
                user_database.collection("user_orders").document(orderID).set(orderDetails);
                //Toast.makeText(checkoutSummaryActivity.this, "jj" + orderID, Toast.LENGTH_SHORT).show();
            }
        });

        Intent i = new Intent(this, CheckoutActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra("orderID", orderID);
        startActivity(i);
        finish();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    public void getAddress() {
        user_database.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    country = documentSnapshot.getString("country");
                    city = documentSnapshot.getString("city");
                    address = documentSnapshot.getString("address");

                    tv_country.setText(country);
                    tv_city.setText(city);
                    tv_address.setText(address);
                }
            }
        });
    }

    public void initializeVaribles() {
        tv_country = (TextView) findViewById(R.id.country);
        tv_city = (TextView) findViewById(R.id.city);
        tv_address = (TextView) findViewById(R.id.address);
        tv_orderTotal = (TextView) findViewById(R.id.orderTotal);
    }

    public void retrieveData() {
        user_database.collection("user_cart").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
                if (e != null) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                CartActivity.progressDialog.dismiss();
                for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                    products productInCart = doc.getDocument().toObject(products.class).getID(doc.getDocument().getId());
                    productsList.add(productInCart);
                    checkoutListAdapter.notifyDataSetChanged();
                    orderTotal = orderTotal + (productInCart.getPrice() * productInCart.getQuantity());
                }
                tv_orderTotal.setText(String.valueOf(orderTotal) + " SAR");
            }
        });
    }
}
