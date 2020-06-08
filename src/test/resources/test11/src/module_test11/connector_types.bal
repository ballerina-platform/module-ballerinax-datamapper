///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                Issue object                                                       //
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
# Represents a github issue.
# + id - Issue identificaion number
# + bodyText - Body of the issue in text format
# + closed - Identifies whether the issue is closed or not
# + closedAt - Date and time when the object was closed
# + createdAt - Date and time when the object was created
# + author - User who created the issue
# + labels - List of labels associated with the issue
# + number - Issue number
# + state - State of the issue (`CLOSED`, `OPEN`)
# + title - Issue title
# + updatedAt - Date and time when the object was updated
# + url - HTTP URL of the issue
# + assignees - List of users assigned to the issue
public type Issue record {|
    string id = "";
    string? bodyText = "";
    string? closed = "";
    string? closedAt = "";
    string createdAt = "";
    Creator author = {};
    Label[] labels = [];
    int number = 0;
    string state = "";
    string title = "";
    string? updatedAt = "";
    string url = "";
    Assignee[] assignees = [];
|};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                               Creator object                                                      //
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
# Represents a github Creator.
# + login - Username of the creator
# + resourcePath - HTTP path of the creator
# + url - HTTP URL of the creator
# + avatarUrl - HTTP URL of the public avatar of the creator
public type Creator record {|
    string login = "";
    string? resourcePath = "";
    string? url = "";
    string? avatarUrl = "";
|};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                               Assignee object                                                     //
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
# Represents a list of assignees to an issue.
# + id - Assignee identification number
# + login - Username of the user
# + email - Email of the user
# + bio - Bio description of the user
# + url - HTTP URL of the user profile
public type Assignee record {|
    string id = "";
    string login = "";
    string email = "";
    string? bio = "";
    string? url = "";
|};

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                 Label object                                                      //
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
# Represents a github label.
# + id - Label identification number
# + name - Label name
# + description - Description of the label
# + color - Color of the label
public type Label record {|
    string id = "";
    string name = "";
    string? description = "";
    string? color = "";
|};