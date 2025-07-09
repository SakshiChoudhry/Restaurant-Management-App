**FOR WORKING API TO CHECK :**

API : https://2x16hfy1zf.execute-api.ap-southeast-1.amazonaws.com/auth/hello


# **For SignUp :**

**API:** https://2x16hfy1zf.execute-api.ap-southeast-1.amazonaws.com/auth/sign-up

**Body needed:**
```json
{
"email": "abc@example.com",
"firstName": "Abc",
"lastName": "Xyz",
"password": "Password@123"
}
```

Response:

    message : {string} -> for Success response
    message : {string}   -> for fail response

# **For SignIn :** 

API : https://2x16hfy1zf.execute-api.ap-southeast-1.amazonaws.com/auth/sign-in

**Body needed:**
```json
{
"email": "abc@example.com",
"password": "Password@123"
}
```
Response:

    accessToken : {string} 
    username:{string}
    role : {string} -> for success
    message : {string}   -> for fail response


# **FOR ADMIN WITH WAITER ROLE:**

**API:** https://jocqs0eg78.execute-api.ap-southeast-1.amazonaws.com/admin/create-waiter

**Body needed:**
```json
{
"email": "abc@example.com"
}
```

Response:

    message : {string} -> for Success response
    message : {string}   -> for fail response



# **PROFILE**

API (GET) :  https://2x16hfy1zf.execute-api.ap-southeast-1.amazonaws.com/auth/profile
