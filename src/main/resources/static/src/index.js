var stompClient = null;
var noOfMessages = 0;
var maxNoOfMessages = 10;

/**
 * This function initialises the served page when the web-socket client gets
 * connected.
 */
function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#connectionStatus")
        .toggleClass("tag-success", connected)
        .toggleClass("tag-danger", !connected)
        .html(`<i class="fa-solid fa-circle"></i>${connected ? 'Connected' : 'Disconnected'}`);
    clearMessages();
}

/**
 * Empties the message table and resets the counter.
 */
function clearMessages() {
    $("#incoming").html("");
    noOfMessages = 0;
    updateMessageCount();
}

/**
 * Keeps the message counter and the empty state in sync with the table.
 */
function updateMessageCount() {
    $("#messageCount").text(noOfMessages);
    $("#messages").toggle(noOfMessages > 0);
    $("#messagesEmpty").toggle(noOfMessages === 0);
}

/**
 * This function handles the connection to the web-socket served by our
 * web-app. Basically, it connects and starts printing out the messages.
 */
function connect() {
    var endpoint = $( "#endpoint option:selected" ).text();
    if(stompClient == null) {
        var socket = new SockJS(window.location.pathname + 'msg-broker-websocket');
        stompClient = Stomp.over(socket);
        stompClient.connect({}, (frame) => {
            setConnected(true);
            showToast(`Listening on the ${endpoint} subject`, 'success');
            stompClient.subscribe('/topic/' + endpoint, (msg) => {
                showMessage(endpoint, JSON.parse(msg.body));
            });
        });
    } else {
        setConnected(true);
        showToast(`Listening on the ${endpoint} subject`, 'success');
        stompClient.subscribe('/topic/' + endpoint, (msg) => {
            showMessage(endpoint, JSON.parse(msg.body));
        });
    }
}

/**
 * This function handles the disconnections from the web-socket served by our
 * web-app. Basically, it unsubscribes and clears out all the messages.
 */
function disconnect() {
    if (stompClient !== null) {
        // Try to remove all the previous subscriptions
        for (const sub in stompClient.subscriptions) {
            if (this.stompClient.subscriptions.hasOwnProperty(sub)) {
                this.stompClient.unsubscribe(sub);
            }
        }
    }
    setConnected(false);
    console.log("Disconnected");
}

/**
 * Simple helper function that appends the incoming messages onto a table.
 * If more messages than the maximum number are receives, it will clear out
 * the table and start over.
 */
function showMessage(endpoint, msg) {
    // For too many messages clear out the incoming table
    if(noOfMessages >= maxNoOfMessages) {
        clearMessages();
    }

    // Pick the identifier based on the type of the publication
    var id = null;
    var content = null;
    var known = true;
    // Handle Navigation Warnings
    if(endpoint === "navigation-warning") {
        id = msg.messageId;
        content = msg.content;
    }
    // Handle Atons, Aton Deletions, Admin Atons and Admin Aton Deletions
    else if(["aton", "aton-delete", "admin-aton", "admin-aton-delete"].includes(endpoint)) {
        id = msg.datasetUID;
        content = msg.content;
    }
    // For any other type
    else {
        known = false;
    }

    // And add the entry to the table
    var row = $("<tr>"
        + "<td>" + (known ? renderIdentifier(id) : '<span class="cell-muted">unknown</span>') + "</td>"
        + "<td class=\"text-nowrap\">" + renderDateTime(new Date()) + "</td>"
        + "<td>" + (known ? "<pre class=\"code-view message-content\"></pre>" : '<span class="cell-muted">N/A</span>') + "</td>"
        + "</tr>");
    // Add the content XML as text
    row.find("pre").text(formatXml(content));
    $("#incoming").prepend(row);

    // Increase the number of shown messages
    noOfMessages++;
    updateMessageCount();
}

/**
 * Standard jQuery initialisation of the page were all buttons are assigned an
 * operation and the form doesn't really do anything.
 */
$(() => {
    $( "#connect" ).click(() => { connect(); });
    $( "#disconnect" ).click(() => { disconnect(); });
    $( "#clear" ).click(() => { clearMessages(); });
    $("form").on('submit', (e) => {
        e.preventDefault();
    });

    // Start from a clean, disconnected state
    setConnected(false);
});
