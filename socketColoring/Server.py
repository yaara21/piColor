import socket
from firebase import firebase
import pyrebase
import base64
from PIL import Image
import io
from io import BytesIO
import torch
import numpy as np
import matplotlib.pyplot as plt
from torchvision import transforms
from skimage.color import rgb2lab, lab2rgb
import threading
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

# my coloring model is based on this notebook with some adjustments
# https://colab.research.google.com/github/moein-shariatnia/Deep-Learning/blob/main/Image%20Colorization%20Tutorial/Image%20Colorization%20with%20U-Net%20and%20GAN%20Tutorial.ipynb

# model loading
model = torch.load("model.pt", map_location=torch.device('cpu'))
model.eval()

# configuration for connecting to firebase
config = {
    "apiKey": "AIzaSyAekGrY56SVM7J2klJtP5pedGAig235mSc",
    "authDomain": "coloringapp-a530a.firebaseapp.com",
    "databaseURL": "https://coloringapp-a530a-default-rtdb.firebaseio.com",
    "projectId": "coloringapp-a530a",
    "storageBucket": "coloringapp-a530a.appspot.com",
    "messagingSenderId": "629922090761",
    "appId": "1:629922090761:web:e35e31132f2677330e8dd8"
}

# globals and const variables
IP = "0.0.0.0"
PORT = 2106
SIZE = 256

num_firebase = firebase.FirebaseApplication("https://coloringapp-a530a-default-rtdb.firebaseio.com/", None)
rfirebase = pyrebase.initialize_app(config)
storage = rfirebase.storage()
online_now = []
online_lock = threading.Lock()





# used functions
#input: string of the number(example - 19)
#output: string of same number -lenth of ten characters (example - 0000000019)
def addZeros(num):
    while len(num) < 10:
        num = "0" + num
    print(num)
    return num

def uploadimage(img_rgb, user_uid):
    global rfirebase, storage
    num = get_user_number(user_uid)
    #create formatted name for the photo in firebase
    fire_newname = user_uid + addZeros(str(num)) + ".jpg"
    bytes_image = Image.fromarray((img_rgb * 255).astype(np.uint8)).convert('RGB')
    img_byte_arr = io.BytesIO()
    bytes_image.save(img_byte_arr, format='JPEG', subsampling=0, quality=100)
    # converts image array to bytesarray
    img_byte_arr = img_byte_arr.getvalue()
    #uploading to firebase
    storage.child(fire_newname).put(img_byte_arr)
    print("uploaded!!!!")

#retrieve user's number of photos
def get_user_number(user_uid):
    global num_firebase, rfirebase
    num_path = "/users/" + user_uid
    num = num_firebase.get(num_path, '')
    num = int(num)
    print(num)
    return num


def display_image(photoarr):
    stream = BytesIO(photoarr)
    image = Image.open(stream).convert("RGB")
    # image = image.convert("LA")
    stream.close()
    # image.show()
    return image

#coloring the image
def coloring(image):
    img5 = image.convert("RGB")
    img5 = img5.resize((SIZE, SIZE), Image.BICUBIC)
    data5 = np.array(img5)
    # converting RGB to L*a*b
    img_lab = rgb2lab(data5).astype("float32")
    img_lab = transforms.ToTensor()(img_lab)
    L = img_lab[[0], ...] / 50. - 1.
    L = L[None, :]
    #coloring
    with torch.no_grad():
        fake_color = model(L)
    fake_color = fake_color * 110
    fake_color = np.squeeze(fake_color)
    tyr = np.append(img_lab[[0], ...], fake_color, 0)
    tyr = np.moveaxis(tyr, 0, -1)
    # converting L*a*b to RGB
    img_rgb = lab2rgb(tyr)
    return img_rgb

# decrtypting message
def decrypt(data):
    data = base64.b64decode(data)
    #fake key
    key = b'\x00\x01\x02\x03\x04\x05\x06\x07\x08\x09\x0a\x0b\x0c\x0d\x0e\x0f'
    cipher = AES.new(key, AES.MODE_CBC)
    #erase help
    message = cipher.decrypt(data)[16:]
    message = unpad(message, 16)
    return message


#create server socket - TCP
Server = socket.socket()
Server.bind((IP, PORT))
Server.listen(1)
print("Server is up and running")


#handling all messages - color, upload, listening
def handling_request(client_socket):
    global online_now, online_lock
    print("in the loop")
    current_data = client_socket.recv(1024)
    data = b""
    while not current_data.endswith(b"\n"):
        data = data + current_data
        current_data = client_socket.recv(1024)
    data = data + current_data
    #decrypting message
    message = decrypt(data)
    print("I got message ")

    datalis = message.split(b"!!!")
    command = datalis[0].decode()
    print("command:::" + command)
    if command == "color":
        print("handling color")
        photoarr = datalis[1]
        photoarr = base64.b64decode(photoarr)
        user_uid = datalis[2].decode()
        #to convert image type
        image = display_image(photoarr)
        #coloring
        img_rgb = coloring(image)
        #uploading to firebase
        uploadimage(img_rgb, user_uid)
        #tries to send message of success to client
        try:
            client_socket.send("good".encode())
            print("image colored and sent")
        except:
            print("client left before receiving")
        client_socket.close()

    else:
        if command == "upload":
            print("handling upload")
            #lock online-users list. for no changes during reading
            online_lock.acquire()
            for cclient in online_now:
                try:
                    #announcing all connected client
                    cclient.send("uploaded\n".encode())
                    print(cclient)
                    print("anounced him")
                except:
                    online_now.remove(cclient)
                    print("couldnt find ")
            #releasing lock
            online_lock.release()
            #updating client that all been announced
            client_socket.send("cool".encode())
            print("finished announcing")
        else:
            if command == "listening":
                print("in listening")
                #checks if lock is locked - and wait if needs
                while online_lock.locked():
                    print("locked")
                # appending client to connected_users list
                online_now.append(client_socket)
                print("online_now:::::::::::")
                print(online_now)


## Server.close()

while True:
    client_socket, address = Server.accept()
    print("got client")
    # New thread - can have multiple clients at the same time
    t = threading.Thread(target=handling_request, args=(client_socket,))
    t.start()
# client_socket.close()
