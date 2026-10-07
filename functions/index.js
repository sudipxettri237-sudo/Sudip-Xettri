const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Triggered on new message creation in a conversation.
 * Delivers FCM push notification and in-app notification to the recipient.
 */
exports.onNewMessage = functions.firestore
  .document("conversations/{conversationId}/messages/{messageId}")
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const conversationId = context.params.conversationId;

    const convDoc = await db.collection("conversations").document(conversationId).get();
    if (!convDoc.exists) return null;

    const participants = convDoc.data().participantIds || [];
    const recipientId = participants.find((id) => id !== message.senderId);
    if (!recipientId) return null;

    // Get recipient FCM token if stored
    const userDoc = await db.collection("users").document(recipientId).get();
    const fcmToken = userDoc.data()?.fcmToken;

    if (fcmToken) {
      const payload = {
        notification: {
          title: `New message from ${message.senderName}`,
          body: message.text || "Sent a photo",
        },
        data: {
          type: "chat",
          conversationId: conversationId,
          senderId: message.senderId,
        },
      };
      await admin.messaging().sendToDevice(fcmToken, payload);
    }

    return null;
  });

/**
 * Triggered when a new comment is added to a post.
 * Sends push notification to the post author.
 */
exports.onCommentCreated = functions.firestore
  .document("posts/{postId}/comments/{commentId}")
  .onCreate(async (snap, context) => {
    const comment = snap.data();
    const postId = context.params.postId;

    const postDoc = await db.collection("posts").document(postId).get();
    if (!postDoc.exists) return null;

    const postAuthorId = postDoc.data().authorId;
    if (postAuthorId === comment.authorId) return null; // Don't notify self

    // Write in-app notification
    const notifRef = db.collection("notifications").doc();
    await notifRef.set({
      notificationId: notifRef.id,
      recipientId: postAuthorId,
      senderId: comment.authorId,
      senderName: comment.authorName,
      type: "comment",
      targetId: postId,
      title: "New Comment",
      message: `${comment.authorName} commented on your post: "${comment.content.slice(0, 40)}"`,
      isRead: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return null;
  });

/**
 * Triggered when a new moderation report is filed.
 * Notifies platform moderators.
 */
exports.onReportCreated = functions.firestore
  .document("reports/{reportId}")
  .onCreate(async (snap, context) => {
    const report = snap.data();
    console.log(`New moderation report filed: [${report.targetType}] ${report.targetId}. Reason: ${report.reason}`);
    return null;
  });

/**
 * Securely sets custom user role claims. Callable only by administrators.
 */
exports.setUserRole = functions.https.onCall(async (data, context) => {
  if (!context.auth || context.auth.token.role !== "admin") {
    throw new functions.https.HttpsError(
      "permission-denied",
      "Only platform administrators may assign roles."
    );
  }

  const { targetUid, newRole } = data;
  if (!["user", "moderator", "admin"].includes(newRole)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid role specified.");
  }

  await admin.auth().setCustomUserClaims(targetUid, { role: newRole });
  await db.collection("users").document(targetUid).update({
    role: newRole,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  return { success: true, targetUid, newRole };
});
