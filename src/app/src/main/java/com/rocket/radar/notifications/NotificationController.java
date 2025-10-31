package com.rocket.radar.notifications;

import java.util.List;


public class NotificationController {


    public NotificationController() {
    }

    public void addNotification(String eventTitle, String notificationType, boolean readStatus, int image, List<Notification> notificationList){
        // temporarily takes in the notificationList
        // TODO: change this controller to add to firebase
        // adds a notification to the list
        Notification newNotification = new Notification(eventTitle, notificationType, readStatus, image);
        notificationList.add(newNotification);

        // updates firebase
        // firebase updates model
        // model updates views
    }
}
