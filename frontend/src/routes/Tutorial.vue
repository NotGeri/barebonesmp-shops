<script setup lang="ts">
import key from '@/assets/images/key.png';
import register from '@/assets/images/register.png';
import copy from '@/assets/images/copy.png';
</script>

<template>
    <div class="bg-darkest rounded-xl p-5 my-3 mx-3 max-w-3xl relative">
        <router-link title="Return" class="absolute right-2 text-3xl hover:no-underline top-1 text-red-400" to="/">
            x
        </router-link>

        <div class="flex flex-col gap-3 mb-3 text-sm">
            <h1 class="text-center">Shop Tracker Tutorial</h1>
            <p>
                The server has a simple plugin that allows you to register shop containers, such as barrels and chests
                to be part of a shop.
            </p>
            <p>Tracked shops and containers will automatically appear in the
                <router-link to="/">shop list</router-link>
            </p>
        </div>

        <hr class="my-7"/>

        <div class="flex flex-col sm:flex-row justify-between items-center sm:items-start text-balance">
            <div class="flex flex-col gap-3">
                <h3>Creating Shop Keys</h3>
                <p>Firstly, find or craft a <span class="text-orange-400">Book and Quill</span>.</p>
                <p>
                    Using the format shown here, give your shop a name, a comma-separated list of owners and an
                    optional description.
                </p>
                <p>
                    The name should be <span class="font-extrabold text-green-400">completely unique</span>, so make
                    sure no other shops are using it already.
                </p>
                <p>
                    This is going to be your shop key. If you lose it, you can create a new one
                    with the same shop name.
                </p>
            </div>
            <img :src="key" alt="Shop key book" class="h-48 mt-1">
        </div>

        <hr class="my-7"/>

        <h3 id="registering">Registering Containers</h3>
        <div class="flex flex-col gap-3">
            <p>
                Ensure you are holding <span class="text-orange-400">your shop key</span> book in your hand.
                This will allow you to access the special menu.
            </p>

            <p>Click on your desired container. This can be a simple chest, barrel or even a shulkerbox.</p>

            <div class="flex flex-col sm:flex-row gap-3 items-center sm:items-start text-balance">
                <div class="flex flex-col gap-3">
                    <p>1. Drag or shift-click your shop key to the top slot. This will register the container with your
                        shop.</p>
                    <p>2. Next, drag or shift-click the item you want to sell to the middle slot.</p>
                    <p>3. Select whether you want to sell X number of pieces, stacks or shulkers of this item.</p>
                    <p>You can use the middle-click to automatically select the most used item from the container.</p>
                    <p>
                        4. Adjust how many of the selected size and for how many diamonds you wish to sell.
                        by<br/>
                        Left clicking <span class="text-orange-400">the item</span> or <span class="text-blue-400">the diamond</span>
                        increases the amount and right clicking decreases it. You can hold shift to
                        step
                        by 10s instead of 1s.
                    </p>
                    <p>
                        5. Click the '<span class="text-green-400">Save Container</span>' button to start tracking
                        it.<br/>
                        You will see a summary, just in case!
                    </p>
                </div>
                <img :src="register" alt="Container register screen" class="h-64 mt-1 object-contain">

            </div>

            <p>Here is an example process:</p>
            <video controls src="/videos/track.mp4"></video>

            <p>
                <span class="font-extrabold text-orange-400">TIP</span>:
                If you wish to sell a service or abstract item instead of a specific block or similar,
                find the closest in-game item that matches with what you are selling and rename it
                in an anvil to the name of your service.
            </p>
        </div>

        <hr class="my-7"/>

        <h3 id="copying-containers">Copying Containers</h3>
        <div class="flex flex-col gap-3">
            <p>
                If you have several containers with the exact same settings, you can copy and paste them with
                the '<span class="text-pink-400">Copy Container</span>' button in the bottom left corner.
            </p>
            <p>
                This will copy all the <i>current</i> (possibly unsaved) settings of the container.
            </p>
            <p>
                Once copied, you can use the '<span class="text-green-400">Paste Container</span>' button in any
                container to paste the exact same settings.
                You will need to use the '<span class="text-green-400">Save Container</span>' button to apply the
                changes.
            </p>
            <img :src="copy" alt="Copying containers" class="h-32 mt-1 object-contain">
            <p>
                The settings will stay on your clipboard until you clear them with shift+click or disconnect.
            </p>
            <video controls src="/videos/copy.mp4"></video>
        </div>

        <hr class="my-7"/>

        <h3 id="updating-stock">Updating The Stock</h3>
        <div class="flex flex-col gap-3">
            <p>
                You do not have to do anything special! Whenever someone closes the container,
                the server will automatically adjust the available stock.
            </p>
            <p>
                If you are expanding and need to sell the same item in several containers,
                you can simply set the exact settings and the list will merge them.
            </p>
        </div>

        <hr class="my-7"/>

        <h3 id="untracking">Untracking Containers</h3>
        <div class="flex flex-col gap-3">
            <p>
                To do this, you can either use the '<span class="text-red-400">Untrack Container</span>' button
                in the edit screen or simply break the container.
                As a precaution, you will need to <span class="text-orange-400">hold sneak</span>
                to confirm you wish to unregister the container by breaking it.
            </p>
            <p>
                If all the containers for a shop are untracked, the shop will be unregistered as well.<br/>
            </p>
            <p>
                You can still use your key to re-create it at any time!
            </p>
            <video controls src="/videos/untrack.mp4"></video>
        </div>

    </div>
</template>

