# Server documentation

The backend will be deployed on the following stack

- AWS EC2 t4g.small instance.
- Using Ubuntu 26.04 LTS 64 bit (ARM)
- 20gb of storage
- 2 cpu, 2gb ram
- Configured with swap so that it doesnt crash when running out of ram

---

## Basic config and connection

The server has configured both ipv4 and ipv6 routing, since an auto stop and start be implemented during off use hours.
The ipv6 address will be configured in cloudflare's DNS, this ipv6 address doesn't rotate on instance stop.

The ipv4 will actually change on every start, but here the only intended use is that the instance has outbound access to the internet without further configuration. (Things like making external requests, pulling docker images, or pulling from github)

## DNS config

The domain used will be [901290.xyz](https://901290.xyz). 
And it will be configured in the DNS, to use the route [ff.901290.xyz](https://ff.901290.xyz).

### SSM

Since the network at TEC doesn't allow ssh traffic natively, we have to move around it.
There option is accessing it via SSM on the AWS console.

> [!NOTE]
> SSH access is still enabled, and accessable anywhere else.

To connect, use the provided credentials accessing the following [link](https://458945172067.signin.aws.amazon.com/console)

Leave the provided Account ID, and enter the `IAM Username` and `Password`. Once logged in follow the next steps to get to the SSM session.

![Login with the account credentials](docs/assets/step1.png)

You will land at the main dashboard, and you will look for the EC2 dashboard, by typing it on the top search bar.

> [!WARNING]
> Make sure, at all type you are in the `United States (Ohio)` region, since that is where the instance is located.

![Go to ec2 dashboard](docs/assets/step2.png)

Then select `Instances (running)`

> [!NOTE]
> Next to `Instances (running)` it has to have a "1", saying that there is 1 running instance. If there is no 1, probably you are in the wrong AWS region, make sure you are on Ohio (us-east-2)

![Enter the running instances list](docs/assets/step3.png)

Tap the instance ID to enter the actual instance, and then click connect on the top right (Next to the instance name)

![Tap the instance ID](docs/assets/step4.png)
![Tap connect](docs/assets/step5.png)

Once in connect, on the section `Choose how to connect` look for `SSM Session Manager`, and tap the bottom right orange button `Connect`

![Tap connect in SSM](docs/assets/step6.png)

That will drop you inside a webview, with a terminal access. This will work the same as having normal terminal access to a computer. Same as ssh'ing into a ubuntu computer or smth.

---
