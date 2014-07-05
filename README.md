CardDAVSyncOutlook
==================

A small tool to synchronize the address book of Outlook with a CardDAV 
implementation (e.g. the address book of an OwnCloud user).

!!!__Make sure to have a backup of any important data before using this tool!__!!!

The 'User Field 1' of all synced Outlook contacts is automatically overwritten! In almost 
all cases it does not contain any data.

# FAQs

## What is the "initialization mode"?

In "initialization mode" the synchronization ID is set for contacts which are 
totally equal.
This is useful when there are already equal contacts in both address books and 
you're running this tool for the first time. After that, start the 
synchronization in default mode to copy new contacts.

## Something is not working / I found a bug!

Feel free to create a new issue on GitHub. The standard output written to 
console is always useful.

