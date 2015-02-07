CardDAVSyncOutlook
==================

A small tool to synchronize the address book of Outlook with a CardDAV 
implementation (e.g. the address book of an OwnCloud user).

!!!__Make sure to have a backup of any important data before using this tool!__!!!

The 'User Field 1' of all synced Outlook contacts is automatically overwritten! In almost 
all cases it does not contain any data.

# Usage

Just extract the Zip archive and run the CardDAVSyncOutlook.jar file (Java >= 7 required).  
From command line: `$ java -jar CardDAVSyncOutlook.jar`  
(command line arguments are shown with `$ java -jar CardDAVSyncOutlook.jar -h`)

On the first run use the "initialization mode" if there are already synced contacts (see below)!

# FAQs

## What is the "initialization mode"?

In "initialization mode" the synchronization ID is set for contacts which are 
totally equal.  
This is useful when there are already equal contacts in both address books and 
you're running this tool for the first time. After that, start the 
synchronization in default mode to copy new contacts.

## I have more than one address book. Can I set up multiple accounts/server URLs?
Yes, by creating multiple configuration files. E.g. copy your `config.properties` file in `conf`
folder to `config_2.properties` and specify the new config from command line:  
`java -jar CardDAVSyncOutlook.jar --config conf\config_2.properties`  
Now change the server URL and Outlook address book (aka "Folder"). Each configuration should use
a different URL and a different folder.

## Something is not working / I found a bug!  
Feel free to create a new issue on GitHub. Debug output is saved to `log.txt` and is always useful. If it
contains private information remove that before posting.
