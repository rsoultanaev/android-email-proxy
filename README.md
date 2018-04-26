# Android Sphinx Proxy

The application requires the
[Java Sphinx library](https://github.com/rsoultanaev/java-sphinx) in order
to build. For instructions on how to build the library, please refer to the
`README.md` file in the source code repository of the Java Sphinx
library. The resulting `.jar` file needs to be placed inside the
`app/libs/` directory of the proxy application's source.

## Bootstrapping

The proxy application needs to be provided with some configuration files in
order to function. These need to be placed inside of the
`app/src/main/assets` directory before building the application. The
files with a `.csv` extension are comma-separated value files. The files
required are as follows:
 
  * `mix_client_config.csv` - Information about the mix network.
    Each row corresponds to a single mix node and is formatted as follows
    (public key is encoded in hex format):
    
    `[id],[hostname],[port],[public key]`

  * `recipient_keys.csv` - Public keys of recipients. Each row
    corresponds to a recipient and is formatted as follows (public key encoded
    in Base64):
    
    `[recipient email address],[public key]`

  * `self_keypair.csv` - Public and private key of this instance of
    the application. Only has one row, in the following format (both public and
    private key encoded in Base64):
    
    `[private key],[public key]`

  * `mailbox.cert` - TLS certificate of the mailbox in the X.509
    format.

## Initial set-up

When the proxy application is first launched, it will display the configuration
activity. Set the parameters as needed, and press "Save config".

After the configuration has been done, start the proxy service by pressing
"Start service" in the main activity. When the proxy is running, launch the mail
client and create a new account. The following explains the configuration using
the K-9 Mail client, but other clients should be similar.

In the settings page for POP3, use `localhost` as the host of the POP3
server, and set the port to where the proxy application's POP3 port is running
(set in the application's configuration). Enter the username and password
configured in the proxy application. Turn off any security options
(communication between the proxy and mail client happens over simple TCP).

In the settings for SMTP, again use `localhost` as the host and as port
use what has been configured in the proxy application, and turn off any security
options and do not require sign-in.

## Usage

After the initial set-up, the account that was set up in the mail box will act
as the interface between the proxy application and the mail client, whenever the
proxy service is active. Messages can be sent as they would with any other
account that uses an SMTP server (but note that the send request will be denied
if the proxy application does not know the recipient's public key). In order to
receive messages, they need to be pulled twice - once by the proxy
application (so that it can reassemble the messages from the packets it
received), and once by the mail client. To make the proxy application pull
messages, press the "Pull from mailbox" button in the main activity.