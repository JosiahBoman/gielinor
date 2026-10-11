package org.gielinorcore.content

import org.gielinorcore.buffer.ReadBuffer
import org.gielinorcore.buffer.WriteBuffer
import org.gielinorcore.cache.Cache
import org.gielinorcore.cache.config.ConfigCodec
import org.gielinorcore.cache.config.ItemDefinition
import org.gielinorcore.cache.config.NpcDefinition
import org.gielinorcore.cache.config.ObjectDefinition
import org.gielinorcore.cache.store.MemoryStore

object FakeCache {
    fun empty() = Cache(MemoryStore())

    private fun <T : Any> stub(archive: Int) = object : ConfigCodec<T> {
        override val archive = archive
        override fun decode(id: Int, buffer: ReadBuffer): T = error("stub")
        override fun encode(value: T, out: WriteBuffer) = error("stub")
    }

    fun codecs() = RevisionCodecs(stub<ItemDefinition>(19), stub<NpcDefinition>(18), stub<ObjectDefinition>(16))
}
