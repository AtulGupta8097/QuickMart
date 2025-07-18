package com.example.groceryapp.Activity;

import static java.lang.String.valueOf;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.groceryapp.GroceryApp;
import com.example.groceryapp.Models.OrdersModel;
import com.example.groceryapp.R;
import com.example.groceryapp.utils.Utils;
import com.example.groceryapp.adapter.CartAdapter;
import com.example.groceryapp.databinding.ActivityCartBinding;
import com.example.groceryapp.databinding.CartItemDesignBinding;
import com.example.groceryapp.roomDatabase.CartProduct;
import com.example.groceryapp.viewModels.UserViewModel;
import com.google.firebase.database.FirebaseDatabase;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity implements PaymentResultListener {
    ActivityCartBinding binding;
    UserViewModel userViewModel;
    CartAdapter cartAdapter;
    int toPay = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCartBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        userViewModel = ((GroceryApp) getApplication()).getUserViewModel();


        Window window = this.getWindow();
        window.setStatusBarColor(getResources().getColor(R.color.white, getApplicationContext().getTheme()));

        setCartRecycler();
        getCartProducts();
        onBackBtnClicked();
        onPayBtnClicked();
        onAddMoreBtnClicked();
        onBrowseTvClicked();

    }

    private void onBrowseTvClicked() {
        binding.browseProductTv.setOnClickListener(V->{
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to_search", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void onAddMoreBtnClicked() {
        binding.addMoreBtn.setOnClickListener(V->{
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("navigate_to_search", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void onBackBtnClicked() {
        binding.backBtn.setOnClickListener(v -> {
            finish();
        });
    }

    private void onPayBtnClicked() {
        binding.payBtn.setOnClickListener(V->{
            String address = userViewModel.getCachedUserAddress();
            if (address == null || address.isEmpty() || address.equals("Not found")) {
                Utils.showToast(this,"Address not selected");
            }
            else {
                Checkout checkout = new Checkout();
                checkout.setKeyID("rzp_test_y8ZHMFKiAeVWpw");

                try {
                    JSONObject options = getJsonObject();
                    checkout.open(this, options);
                } catch(Exception e) {
                    Log.e("Payment Error", "Error in starting Razorpay Checkout", e);
                }
            }
        });

    }

    @NonNull
    private JSONObject getJsonObject() throws JSONException {
        JSONObject options = new JSONObject();
        options.put("name", "QuickMart");
        options.put("description", "Test Transaction");
        options.put("currency", "INR");
        options.put("amount", toPay*100); // amount in paise (₹500.00)

        JSONObject preFill = new JSONObject();
        preFill.put("email", "atulgupta8070@gmail.com");
        preFill.put("contact", "+917718094564");
        options.put("prefill", preFill);
        return options;
    }

    private void getCartProducts() {
        userViewModel.getCartProducts().observe(this, cartProductList -> {
            cartAdapter.submitList(cartProductList);
            updateCartTotal(cartProductList);
        });
    }

    private void setCartRecycler() {
        cartAdapter = new CartAdapter(new CartProductListener() {
            @Override
            public void onPlusBtnClicked(int item, CartProduct cartProduct, CartItemDesignBinding cartBinding) {
                int currentNumber = Integer.parseInt(cartBinding.productNumbers.getText().toString());
                int cartInc = currentNumber + item;

                if (cartInc <= cartProduct.getProductStock()) {
                    CartProduct updatedProduct = new CartProduct(
                            cartProduct.getProductId(),
                            cartProduct.getProductTitle(),
                            cartProduct.getProductCategory(),
                            cartProduct.getProductImageUri(),
                            cartProduct.getProductQuantity(),
                            cartProduct.getProductPrice(),
                            cartProduct.getProductStock(),
                            cartInc,
                            cartProduct.getBuyCount(),
                            cartProduct.getRatingCount(),
                            cartProduct.getAverageRating()
                    );
                    cartBinding.productNumbers.setText(valueOf(cartInc));
                    Utils.animateNumberChange(cartBinding.productNumbers, true);
                    Utils.vibrate(CartActivity.this, 50);


                    Integer badgeCurrent = userViewModel.getBadgeCartCount().getValue();
                    badgeCurrent = badgeCurrent != null ? badgeCurrent : 0;
                    userViewModel.setBadgeCartCount(badgeCurrent + 1);

                    saveCartProductInDB(updatedProduct);
                    Log.d("error",updatedProduct.toString());
                    userViewModel.updateCartProductItem(updatedProduct);
                } else {
                    Utils.showToast(CartActivity.this, "Currently we have only " + currentNumber + " item(s) available");
                }
            }

            @Override
            public void onMinusBtnClicked(int item, CartProduct cartProduct, CartItemDesignBinding cartBinding) {
                int currentNumber = Integer.parseInt(cartBinding.productNumbers.getText().toString());
                int cartDec = currentNumber - item;

                if (cartDec >= 0) {
                    CartProduct updatedProduct = new CartProduct(
                            cartProduct.getProductId(),
                            cartProduct.getProductTitle(),
                            cartProduct.getProductCategory(),
                            cartProduct.getProductImageUri(),
                            cartProduct.getProductQuantity(),
                            cartProduct.getProductPrice(),
                            cartProduct.getProductStock(),
                            cartDec,
                            cartProduct.getBuyCount(),
                            cartProduct.getRatingCount(),
                            cartProduct.getAverageRating()
                    );
                    cartBinding.productNumbers.setText(valueOf(cartDec));
                    Utils.animateNumberChange(cartBinding.productNumbers, false);
                    Utils.vibrate(CartActivity.this, 50);


                    Integer badgeCurrent = userViewModel.getBadgeCartCount().getValue();
                    badgeCurrent = badgeCurrent != null ? badgeCurrent : 0;
                    userViewModel.setBadgeCartCount(Math.max(badgeCurrent - 1, 0));

                    saveCartProductInDB(updatedProduct);
                    Log.d("error",updatedProduct.toString());
                    userViewModel.updateCartProductItem(updatedProduct);

                    if (cartDec == 0) {
                        userViewModel.deleteCartProduct(cartProduct.getProductId());
                        FirebaseDatabase.getInstance().getReference()
                                .child("Admins").child("AdminInfo").child("userCarts")
                                .child(Utils.getUserPhoneNumber())
                                .child(cartProduct.getProductId())
                                .removeValue();
                    }
                }
            }

            private void saveCartProductInDB(CartProduct cartProduct) {
                userViewModel.updateCartProduct(cartProduct);
            }
        });

        binding.cartRecycler.setAdapter(cartAdapter);
    }




    @SuppressLint("DefaultLocale")
    private void updateCartTotal(List<CartProduct> cartProducts) {
        if(!cartProducts.isEmpty()){
            binding.noCartCv.setVisibility(View.GONE);
            binding.addMoreLl.setVisibility(View.VISIBLE);
            binding.billCv.setVisibility(View.VISIBLE);
            binding.payBtn.setVisibility(View.VISIBLE);

        }
        else{
            binding.noCartCv.setVisibility(View.VISIBLE);
            binding.addMoreLl.setVisibility(View.GONE);
            binding.billCv.setVisibility(View.GONE);
            binding.payBtn.setVisibility(View.GONE);

        }
        int total = 0;
        int deliveryCharge = 0;

        for (CartProduct product : cartProducts) {
            total += product.getProductPrice() * product.getItemCount();
        }
        binding.itemTotal.setText(String.valueOf(total));
        if(total<250){
            deliveryCharge = 45;
            toPay = total+deliveryCharge;
            binding.cancelDileveryFee.setPaintFlags(0);
            binding.dileveryFee.setVisibility(View.GONE);
            binding.cancelDileveryFee.setText(String.format("₹%d",deliveryCharge));
        }
        else{
            toPay = total;
            binding.dileveryFee.setVisibility(View.VISIBLE);
            binding.cancelDileveryFee.setPaintFlags(binding.cancelDileveryFee.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            binding.cancelDileveryFee.setTextColor(getResources().getColor(R.color.dark_grey, this.getTheme()));
            binding.dileveryFee.setText(String.format("₹%d", deliveryCharge));
        }
        binding.overallPrice.setText(String.format("₹%d", toPay));
    }


    @Override
    public void onPaymentSuccess(String paymentId) {
        userViewModel.resetProductsAndCartBadge();

        Utils.observeOnce(userViewModel.getCartProducts(), this, cartProducts -> {
            for (CartProduct product : cartProducts) {
                userViewModel.increaseBuyCountInFirebase(product, product.getItemCount());
            }

            // Save order and go to OrderSuccessActivity
            saveOrder(cartProducts);
        });
    }



    private void saveOrder(List<CartProduct> cartProducts) {
        String address = userViewModel.getCachedUserAddress();
        String date = getCurrentDateTime();
        String orderId = Utils.getUniqueId();

        OrdersModel order = new OrdersModel(
                cartProducts,
                address,
                Utils.getUserPhoneNumber(),
                date,
                orderId,
                0,
                "Online"
        );

        userViewModel.saveOrder(order);

        userViewModel.deleteAllCartProductFromRoomDB();
        userViewModel.deleteCartProductFromFirebaseDb();

        // Open Order Success page with toPay value we already have
        Intent intent = new Intent(this, OrderSuccessActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("date", date);
        intent.putExtra("address", address);
        intent.putExtra("price", String.valueOf(toPay));  // ✅ using existing toPay variable here
        startActivity(intent);
        finish();
    }


    @Override
    public void onPaymentError(int i, String s) {
        Utils.showToast(this,"Payment failed");
    }
    public String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

}
