# Like / Delete Message

## Like Message 👍

```kotlin
ChatClient.getInstance().likeMessage("channelId", "messageId") {
    // Handle on Suceess
}
```

## Delete Message 🗑️

```kotlin
 ChatClient.getInstance().deleteMessage(channelId, message, {
        //Handle OnSuccess of Delete
     }, {
        //Handle Exception of Delete
})
```
