package client;

import Tools.CalendarUtil;
import Tools.FileReaderWriter;
import Tools.UdpSend;
import requests.*;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client implements Runnable {

    private static final AtomicInteger countID = new AtomicInteger(0);  //Thread safe auto increment for RequestNumber

    private final int serverPort = 9997;
    private final int millisBetweenSaves = 2000;

    private String clientName;

    private static DatagramSocket ds;

    private InetSocketAddress serverAddress;

    private ArrayList<ClientMeeting> meetings;
    private HashMap<String, Boolean> availability;

    private List<String> ClientLog;

    public Client(String clientName) {
        this.clientName = clientName;
        this.availability = new HashMap<>();

        this.meetings = new ArrayList<>();
        this.ClientLog = new ArrayList<>();

        try {
            ds = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }

        try {
            this.serverAddress = new InetSocketAddress(InetAddress.getLocalHost(), serverPort);
        } catch (UnknownHostException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void sendMessageToServer(String message) throws IOException {

        // convert the String input into the byte array.
        String first = "Request_1_2019,10,6,8_2_59000_asd";
        RequestMessage firstRequest = new RequestMessage();
        firstRequest.deserialize(first);
        UdpSend.sendMessage(firstRequest.serialize(), ds, serverAddress);
//        byte buf[] = message.getBytes();
//        byte[] buffer = new byte[100];
//        DatagramPacket DpSend = new DatagramPacket(buf, buf.length, InetAddress.getLocalHost(), 9997);
//        ds.send(DpSend);
//        System.out.println("MESSAGE SENT");


    }

    @Override
    public void run() {

        ClientListen clientListen = new ClientListen(); //Adding thread for client to listen to server messages
        Thread listenThread = new Thread(clientListen);
        listenThread.start();

        ClientSave clientSave = new ClientSave(); //Adding thread for client to save it's progress
        Thread saveThread = new Thread(clientSave);
        saveThread.start();

        sendRegistrationMessage();

        System.out.println("Local port is: " + ds.getLocalPort());

    }

    private void checkState() {
        //If meeting list is not empty
        if (!meetings.isEmpty()) {
            //Get how many meetings this client is part of
            int meetingNumbers = meetings.size();
            System.out.println("You are a part of " + meetingNumbers + ", which meeting do you want to choose?");
            System.out.println("Type 'None' to not select any of the current meetings");
            Scanner scanner = new Scanner(System.in);
            String answer = scanner.nextLine();
            if (!answer.equals("None")) {
                try {
                    if (Integer.parseInt(answer) <= meetingNumbers) {
                        //Use the meeting the user chose
                        ClientMeeting clientMeeting = meetings.get(Integer.parseInt(answer));

                        if (clientMeeting.getUserType() && clientMeeting.getState()) {
                            //Organizer and meeting is confirmed
                            //Organizer can cancel the meeting
                            System.out.println("This meeting is confirmed");
                            System.out.println("Meeting number: " + clientMeeting.getMeetingNumber());
                            System.out.println("Type 'Cancel_MeetingNumber' to cancel the meeting");

                        } else if (!clientMeeting.getUserType() && clientMeeting.getState() && clientMeeting.isCurrentAnswer()) {
                            //Invitee, meeting is confirmed and current answer is accepted
                            //At confirm message, meeting is confirmed, can only withdraw
                            System.out.println("This meeting is confirmed");
                            System.out.println("Meeting number: " + clientMeeting.getMeetingNumber());
                            System.out.println("Type 'Withdraw_MeetingNumber' to withdraw from the meeting");
                        } else if (!clientMeeting.getUserType() && clientMeeting.getState() && !clientMeeting.isCurrentAnswer()) {
                            //Invitee, meeting is confirmed and current answer is not accepted
                            //At add stage
                            System.out.println("This meeting is confirmed");
                            System.out.println("Meeting number: " + clientMeeting.getMeetingNumber());
                            System.out.println("Type 'Add_MeetingNumber' to add yourself to the meeting");

                        }


                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Type in your new request");
            }
        }
    }

    public void sendRequest(Calendar calendar, int minimum, List<String> participants, String topic) {

        if (!availability.containsKey(CalendarUtil.calendarToString(calendar))) {

            //Create a RequestMessage
            RequestMessage requestMessage = new RequestMessage(countID.incrementAndGet(), calendar, minimum, participants, topic);

            //Add the sent request to my list
            synchronized (meetings) {
                meetings.add(new ClientMeeting(requestMessage));
            }

            synchronized (availability) {
                availability.put(CalendarUtil.calendarToString(calendar), true);
            }

            //Send the RequestMessage to the server
            UdpSend.sendMessage(requestMessage.serialize(), ds, serverAddress);

            Calendar cal = Calendar.getInstance();
            String currentTime = "Client[" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.YEAR) + " "
                    + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "]: ";
            FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Request from '" + clientName + "' " + requestMessage.serialize() + "\n", true);
            ClientLog.add("Request from '" + clientName + "' " + requestMessage.serialize());

        }

    }

    public void sendAccept(int meetingNumber) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == meetingNumber && !meetings.get(i).getState()) {
                synchronized (meetings) {
                    meetings.get(i).setCurrentAnswer(true);
                }

                AcceptMessage acceptMessage = new AcceptMessage(meetingNumber);
                UdpSend.sendMessage(acceptMessage.serialize(), ds, serverAddress);

                Calendar cal = Calendar.getInstance();
                String currentTime = "Client[" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.YEAR) + " "
                        + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "]: ";
                FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Accept from '" + clientName + "' " + acceptMessage.serialize() + "\n", true);
                ClientLog.add("Accept from '" + clientName + "' " + acceptMessage.serialize());

            }
        }

    }

    public void sendReject(int meetingNumber) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == meetingNumber && !meetings.get(i).getState()) {
                synchronized (meetings) {
                    meetings.get(i).setCurrentAnswer(false);
                }

                RejectMessage rejectMessage = new RejectMessage(meetingNumber);
                UdpSend.sendMessage(rejectMessage.serialize(), ds, serverAddress);

                Calendar cal = Calendar.getInstance();
                String currentTime = "Client[" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.YEAR) + " "
                        + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + "]: ";
                FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Reject from '" + clientName + "' " + rejectMessage.serialize() + "\n", true);
                ClientLog.add("Reject from '" + clientName + "' " + rejectMessage.serialize());
            }
        }

    }

    public void sendWithdraw(int meetingNumber) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == meetingNumber && meetings.get(i).getState()
                    && !meetings.get(i).getUserType()) {

                synchronized (meetings) {
                    meetings.get(i).setCurrentAnswer(false);
                }

                WithdrawMessage withdrawMessage = new WithdrawMessage(meetingNumber);
                UdpSend.sendMessage(withdrawMessage.serialize(), ds, serverAddress);

                Calendar calendar = Calendar.getInstance();
                String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                        + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Add from '" + clientName + "' " + withdrawMessage.serialize() + "\n", true);
                ClientLog.add(currentTime + "Add from '" + clientName + "' " + withdrawMessage.serialize());

            }
        }

    }

    public void sendAdd(int meetingNumber) {

        for (int i = 0; i < meetings.size(); i++) {

            if (meetingNumber == meetings.get(i).getMeetingNumber()) {
                if (!meetings.get(i).getUserType()) {
                    meetings.get(i).setCurrentAnswer(true);

                    System.out.println("Sending");

                    AddMessage addMessage = new AddMessage(meetingNumber);
                    UdpSend.sendMessage(addMessage.serialize(), ds, serverAddress);

                    Calendar calendar = Calendar.getInstance();
                    String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                            + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                    FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Add from '" + clientName + "' " + addMessage.serialize() + "\n", true);
                    ClientLog.add(currentTime + "Add from '" + clientName + "' " + addMessage.serialize());

                }
                return;
            }
        }

    }

    public void sendRequesterCancel(int meetingNumber) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == meetingNumber) {
                if (meetings.get(i).getUserType() && meetings.get(i).getState()) {

                    RequesterCancelMessage requesterCancelMessage = new RequesterCancelMessage(meetingNumber);
                    UdpSend.sendMessage(requesterCancelMessage.serialize(), ds, serverAddress);

                    Calendar calendar = Calendar.getInstance();
                    String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                            + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                    FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Cancel from '" + clientName + "' " + requesterCancelMessage.serialize() + "\n", true);
                    ClientLog.add(currentTime + "Cancel from '" + clientName + "' " + requesterCancelMessage.serialize());

                }

                return;
            }
        }

    }

    private void sendRegistrationMessage() {

        RegisterMessage registerMessage = null;

        try {
            registerMessage = new RegisterMessage(clientName, new InetSocketAddress(InetAddress.getLocalHost(), ds.getLocalPort()));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        UdpSend.sendMessage(registerMessage.serialize(), ds, serverAddress);

        Calendar calendar = Calendar.getInstance();
        String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
        FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Register from '" + clientName + "' " + registerMessage.serialize() + "\n", true);
        ClientLog.add(currentTime + "Register from '" + clientName + "' " + registerMessage.serialize());

    }

    private void handleDenied(DeniedMessage message) {  //Room Unavailable Message

        //Check if request RQ# exists inside its list of request and is the owner
        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getRequestNumber() == message.getRequestNumber()) {
                //If true, Delete the request that was just sent to the server
                synchronized (meetings) {
                    meetings.remove(i);

                    Calendar calendar = Calendar.getInstance();
                    String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                            + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                    FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Denied " + message.serialize() + "\n", true);
                    ClientLog.add(currentTime + "Denied " + message.serialize());
                }
                return;
            }
        }

        //If false, Server sent incorrect request
        System.out.println("Server sent denied for a non-existant request");

    }

    private void handleInvite(InviteMessage message) {
        System.out.println("Got Invite");
        //Add the new request into your list and make it a standby status meeting
        ClientMeeting newMeeting = new ClientMeeting(message);

        if (!message.getRequester().equals(clientName)) {

            if (!availability.containsKey(CalendarUtil.calendarToString(newMeeting.getCalendar()))) {
                newMeeting.setCurrentAnswer(true);
                synchronized (meetings) {

                    meetings.add(newMeeting);
                }

                synchronized (availability){
                    availability.put(CalendarUtil.calendarToString(newMeeting.getCalendar()), true);
                }

                //Send Accept
                sendAccept(newMeeting.getMeetingNumber());

            } else {
                newMeeting.setCurrentAnswer(false);
                synchronized (meetings) {
                    meetings.add(newMeeting);
                }

                //Send Reject
                sendReject(newMeeting.getMeetingNumber());
            }

        } else {

            if(!availability.containsKey(CalendarUtil.calendarToString(newMeeting.getCalendar()))) {
                for (int i = 0; i < meetings.size(); i++) {
                    if (meetings.get(i).getCalendar().equals(message.getCalendar())) {
                        meetings.get(i).setMeetingNumber(message.getMeetingNumber());
                    }
                }

                //Send Accept
                sendAccept(newMeeting.getMeetingNumber());
            } else {
                sendReject(newMeeting.getMeetingNumber());
            }

        }

        Calendar calendar = Calendar.getInstance();
        String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
        FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Invite for '" + clientName + "'" + message.serialize() + "\n", true);
        ClientLog.add(currentTime + "Invite for '" + clientName + "'" + message.serialize());

    }

    private void handleConfirm(ConfirmMessage message) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == message.getMeetingNumber()) {
                if (!meetings.get(i).getState() && !meetings.get(i).getUserType()) {
                    synchronized (meetings) {
                        meetings.get(i).receiveConfirmMessage(message);
                        Calendar calendar = Calendar.getInstance();
                        String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                        FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Confirm from '" + clientName + "' " + message.serialize() + "\n", true);
                        ClientLog.add(currentTime + "Confirm from '" + clientName + "' " + message.serialize());
                    }
                }
                return;
            }
        }

    }

    private void handleServerCancel(ServerCancelMessage message) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == message.getMeetingNumber()) {
                if (!meetings.get(i).getState() && !meetings.get(i).getUserType()) {
                    System.out.println("Meeting " + message.getMeetingNumber() + " was cancelled for this reason : " + message.getReason());

                    synchronized (availability) {
                        availability.remove(CalendarUtil.calendarToString(meetings.get(i).getCalendar()));
                    }

                    synchronized (meetings) {
                        meetings.remove(i);
                    }

                    Calendar calendar = Calendar.getInstance();
                    String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                            + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                    FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Cancel for '" + clientName + "'" + message.serialize() + "\n", true);
                    ClientLog.add(currentTime + "Cancel for '" + clientName + "'" + message.serialize());
                }
            }
        }

    }

    private void handleScheduled(ScheduledMessage message) {

        System.out.println(clientName + " received Schedule : " + message);

        //Check if request RQ# is part of my list and is in standby (Only Host should receive)
        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getRequestNumber() == message.getRequestNumber()) {
                if (!meetings.get(i).getState() && meetings.get(i).getUserType()) {
                    //Change Meeting to complete and change info in meeting
                    meetings.get(i).receiveScheduledMessage(message);

                    Calendar calendar = Calendar.getInstance();
                    String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                            + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                    FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Scheduled for '" + clientName + "' " + message.serialize() + "\n", true);
                    ClientLog.add(currentTime + "Scheduled for '" + clientName + "' " + message.serialize());
                    return;
                }
                System.out.println("!meetings.get(i).getState() && meetings.get(i).getUserType() is false");
                return;
            }
            System.out.println(meetings.get(i).getRequestNumber()  + " != " + message.getRequestNumber());
        }

    }

    private void handleNotScheduled(NotScheduledMessage message) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getRequestNumber() == message.getRequestNumber()) {
                if (!meetings.get(i).getState() && meetings.get(i).getUserType()) {
                    synchronized (meetings) {
                        meetings.remove(i);
                        Calendar calendar = Calendar.getInstance();
                        String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                        FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Not scheduled for '" + clientName + "' " + message.serialize() + "\n", true);
                        ClientLog.add(currentTime + "Not scheduled for '" + clientName + "' " + message.serialize());
                    }
                }
            }
        }

    }

    private void handleAdded(AddedMessage message) {

        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == message.getMeetingNumber()) {
                if (meetings.get(i).getState() && meetings.get(i).getUserType()) {
                    synchronized (meetings) {
                        meetings.get(i).getAcceptedMap().put(message.getSocketAddress(), true);

                        Calendar calendar = Calendar.getInstance();
                        String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                        FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Add for '" + clientName + "' " + message.serialize() + "\n", true);
                        ClientLog.add(currentTime + "Add for '" + clientName + "' " + message.serialize());
                    }
                }
            }
        }

    }

    private void handleRoomChange(RoomChangeMessage message) {
        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == message.getMeetingNumber()) {
                if (meetings.get(i).getState()) {
                    synchronized (meetings) {
                        meetings.get(i).setRoomNumber(message.getNewRoomNumber());

                        Calendar calendar = Calendar.getInstance();
                        String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                        FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Room change for '" + clientName + "' " + message.serialize() + "\n", true);
                        ClientLog.add(currentTime + "Room change for '" + clientName + "' " + message.serialize());
                    }
                }
            }
        }
    }

    private void handleServerWidthdraw(ServerWidthdrawMessage message) {
        for (int i = 0; i < meetings.size(); i++) {
            if (meetings.get(i).getMeetingNumber() == message.getMeetingNumber()) {
                if (meetings.get(i).getState() && meetings.get(i).getUserType()) {
                    synchronized (meetings) {
                        meetings.get(i).getAcceptedMap().remove(Integer.parseInt(message.getIpAddress()));

                        availability.remove(CalendarUtil.calendarToString(meetings.get(i).getCalendar()));

                        Calendar calendar = Calendar.getInstance();
                        String currentTime = "Client[" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR) + " "
                                + calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + ":" + calendar.get(Calendar.SECOND) + "]: ";
                        FileReaderWriter.WriteFile("logClient" + clientName, currentTime + "Room change for '" + clientName + "' " + message.serialize() + "\n", true);
                        ClientLog.add(currentTime + "Room change for '" + clientName + "' " + message.serialize());

                    }
                }
            }
        }
    }

    public String getClientName() {
        return clientName;
    }

    public ArrayList<Integer> getWidthdrawNumbers() {

        ArrayList<Integer> result = new ArrayList<>();

        for (ClientMeeting meeting : meetings) {
            if (!meeting.getUserType() && meeting.getState() && meeting.isCurrentAnswer()) {
                result.add(meeting.getMeetingNumber());
            }
        }

        return result;
    }

    public ArrayList<Integer> getAddNumbers() {
        ArrayList<Integer> result = new ArrayList<>();

        for (ClientMeeting meeting : meetings) {
            if (!meeting.getUserType() && meeting.getState() && !meeting.isCurrentAnswer() &&
                    !availability.containsKey(CalendarUtil.calendarToString(meeting.getCalendar()))) {
                result.add(meeting.getMeetingNumber());
            }
        }

        return result;
    }

    public ArrayList<Integer> getRequesterNumbers() {
        ArrayList<Integer> result = new ArrayList<>();

        for (ClientMeeting meeting : meetings) {
            if (meeting.getUserType() && meeting.getState()) {
                result.add(meeting.getMeetingNumber());
            }
        }

        return result;
    }

    public List<String> getLog() {
        return ClientLog;
    }

    public void setServerAddress(String[] split) {

        this.serverAddress = new InetSocketAddress(split[0], Integer.parseInt(split[1]));

    }

    public class ClientListen implements Runnable {

        public ClientListen() {
        }

        @Override
        public void run() {

            //Like server, will listen to ip
            /**Create new server and binds to a free port. From source of the internet
             * the range should be 49152 - 65535.*/

            /**The port address is chosen randomly*/
            byte[] buffer = new byte[100];
            /**Messages here and sends to client*/
            while (true) {
                DatagramPacket DpReceive = new DatagramPacket(buffer, buffer.length);   //Create Datapacket to receive the data
                try {
                    ds.receive(DpReceive);        //Receive Data in Buffer
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                String message = new String(DpReceive.getData(), 0, DpReceive.getLength());
                System.out.println("Server says: " + message);
                /**NEED TO ADD IN TIMEOUT OPTIONS TO RESEND THE MESSAGE. HAVE YET TO
                 * COMPLETE THIS PORTION OF THE CODE
                 *
                 * Add in Thread and feed in the message*/
                //This would be the thread managing method
                new Thread(new ClientHandle(message)).start();

            }


        }
    }

    //Class for when the Client receives a message
    public class ClientHandle implements Runnable {

        String message;

        public ClientHandle(String message) {
            this.message = message;
        }

        @Override
        public void run() {

            String[] receivedMessage = message.split("\\$");
            int messageType = Integer.parseInt(receivedMessage[0]);
            RequestType receivedRequestType = RequestType.values()[messageType];

            switch (receivedRequestType) {
                case Denied:
                    DeniedMessage deniedMessage = new DeniedMessage();
                    deniedMessage.deserialize(message);
                    handleDenied(deniedMessage);
                    break;
                case Invite:
                    InviteMessage inviteMessage = new InviteMessage();
                    inviteMessage.deserialize(message);
                    handleInvite(inviteMessage);
                    break;
                case Confirm:
                    ConfirmMessage confirmMessage = new ConfirmMessage();
                    confirmMessage.deserialize(message);
                    handleConfirm(confirmMessage);
                    break;
                case ServerCancel:
                    ServerCancelMessage serverCancelMessage = new ServerCancelMessage();
                    serverCancelMessage.deserialize(message);
                    handleServerCancel(serverCancelMessage);
                    break;
                case Scheduled:
                    ScheduledMessage scheduledMessage = new ScheduledMessage();
                    scheduledMessage.deserialize(message);
                    handleScheduled(scheduledMessage);
                    break;
                case NotScheduled:
                    NotScheduledMessage notScheduledMessage = new NotScheduledMessage();
                    notScheduledMessage.deserialize(message);
                    handleNotScheduled(notScheduledMessage);
                    break;
                case Added:
                    AddedMessage addedMessage = new AddedMessage();
                    addedMessage.deserialize(message);
                    handleAdded(addedMessage);
                    break;
                case RoomChange:
                    RoomChangeMessage roomChangeMessage = new RoomChangeMessage();
                    roomChangeMessage.deserialize(message);
                    handleRoomChange(roomChangeMessage);
                    break;
                case ServerWidthdraw:
                    ServerWidthdrawMessage serverWidthdrawMessage = new ServerWidthdrawMessage();
                    serverWidthdrawMessage.deserialize(message);
                    handleServerWidthdraw(serverWidthdrawMessage);
                    break;

            }

        }

    }

    private String serialize() {

        String result = ""; //meetings ArrayList

        for (int i = 0; i < meetings.size(); i++) {
            if (i == 0) {
                result += meetings.get(i).serialize();
                continue;
            }

            result += ";" + meetings.get(i).serialize();

        }

        result += "_";

        for (String s : availability.keySet()) { //Availability Hashmap
            result += s + ";";
        }

        return result;

    }

    public class ClientSave implements Runnable {

        @Override
        public void run() {

            while (true) {
                try {
                    Thread.sleep(millisBetweenSaves);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                FileReaderWriter.WriteFile("saveFile_" + clientName, serialize(), false);
            }

        }

    }

    public void restoreFromSave(String saveFile) {

        ArrayList<String> messageList = FileReaderWriter.ReadFile(saveFile);

        String message = "";

        for (String msgPortion : messageList) {
            message += msgPortion;
        }

        System.out.println("Message: " + message);

        String[] subMessage = message.split("_");

        if (subMessage.length > 0 && !subMessage[0].isEmpty()) {
            String[] meetings = subMessage[0].split(";");

            for (String meeting : meetings) {

                if(!meeting.isEmpty()){
                    ClientMeeting newMeeting = new ClientMeeting();
                    newMeeting.deserialize(meeting);
                    this.meetings.add(newMeeting);
                }

            }
        }

        if (subMessage.length > 1 && !subMessage[1].isEmpty()) {
            String[] availability = subMessage[1].split(";");

            for (String available : availability) {
                if(!available.isEmpty()){
                    this.availability.put(available, true);

                }
            }
        }

    }

}