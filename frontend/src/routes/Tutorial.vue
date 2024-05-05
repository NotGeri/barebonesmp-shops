<script setup lang="ts">
import key from '@/assets/images/key.png';
import register from '@/assets/images/register.png';
</script>

<template>
    <div class="bg-darkest rounded-xl p-5 my-3 mx-3 max-w-3xl relative">
        <router-link title="Return" class="absolute right-2 text-3xl hover:no-underline top-1 text-red-400" to="/">
            x
        </router-link>

        <h1 class="text-center">Shop Tracker Tutorial</h1>

        <div class="flex flex-col sm:flex-row justify-between items-center sm:items-start text-balance">
            <div class="flex flex-col gap-3">
                <h3>Creating Shop Keys</h3>
                <p>Firstly, find or craft a <span class="text-orange-400">Book and Quill</span>.</p>
                <p>
                    Using the format shown here, give your shop a name, a comma-separated list of owners and an
                    optional description.
                </p>
                <p>
                    This is going to be your shop key. You can always create a new one, but it would be best to
                    store
                    this in a safe place.
                </p>
            </div>
            <img :src="key" alt="Shop key book" class="h-48 mt-1">
        </div>

        <hr class="my-7"/>

        <h3 id="registering">Registering Containers</h3>
        <div class="flex flex-col gap-3">
            <p>
                Ensure you are holding a <span class="text-orange-400">Tripwire</span> in your hand.
                This will allow you to access the special menu.
            </p>

            <p>Right click on your desired container. This can be a simple chest, barrel or even a shulkerbox.</p>

            <div class="flex flex-col sm:flex-row gap-3 items-center sm:items-start text-balance">
                <div class="flex flex-col gap-3">
                    <p>1. Drag your shop key to the top slot. This will register the container with your shop.</p>
                    <p>2. Next, drag the item you want to sell to the middle slot.</p>
                    <p>3. Select whether you want to sell X number of pieces, stacks or shulkers of this item.</p>
                    <p>
                        4. Adjust how many of the selected size and for how many diamonds you wish to
                        sell.<br/>
                        Left clicking increases the amount and right clicking decreases it. You can hold shift to
                        step
                        by 10s instead of 1s.
                    </p>
                    <p>
                        5. Click the 'Save Container' button to start tracking it.<br/>
                        You will see a summary, just in case!
                    </p>
                    <p class="text-xs text-muted">You can adjust either of these settings at any time.</p>
                </div>
                <img :src="register" alt="Container register screen" class="h-64 mt-1 object-contain">

            </div>

            <p>
                <span class="font-extrabold text-orange-400">TIP</span>: If you wish to sell a service or abstract
                item instead of a specific block or similar,
                find the closest in-game item that matches with what you are selling.
            </p>
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
                To do this, you can either use the 'Untrack Container' in the edit screen or simply break
                the container. As a precaution, you will need to <span class="text-orange-400">hold sneak</span>
                to confirm you wish to unregister the container by breaking it.
            </p>
            <p>
                If all the containers for a shop are untracked, the shop will be unregistered as well.<br/>
                You can still use your key to re-create it at any time!
            </p>
        </div>

    </div>
</template>

