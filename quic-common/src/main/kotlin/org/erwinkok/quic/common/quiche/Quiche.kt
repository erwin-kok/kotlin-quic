package org.erwinkok.quic.common.quiche

import io.github.oshai.kotlinlogging.KotlinLogging
import org.erwinkok.quic.common.quiche.NativeHelper.C_BOOL
import org.erwinkok.quic.common.quiche.NativeHelper.C_BYTE
import org.erwinkok.quic.common.quiche.NativeHelper.C_INT
import org.erwinkok.quic.common.quiche.NativeHelper.C_LONG
import org.erwinkok.quic.common.quiche.NativeHelper.C_POINTER
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.MemorySegment

private val logger = KotlinLogging.logger {}

@Suppress("ktlint:standard:function-naming")
object Quiche {
    private const val EXPECTED_QUICHE_VERSION = "0.24.4"

    init {
        val quicheVersion = quiche_version().getUtf8String(0L)
        require(quicheVersion == EXPECTED_QUICHE_VERSION) {
            "Unexpected Native Quiche version: $quicheVersion, expected $EXPECTED_QUICHE_VERSION"
        }
        if (logger.isDebugEnabled()) {
            logger.debug { "Loaded Native Quiche version: $quicheVersion" }

            val cb = NativeHelper.upcallMemorySegment<LoggingCallback>(
                "log",
                LoggingCallback.INSTANCE,
                FunctionDescriptor.ofVoid(C_POINTER, C_POINTER),
                LoggingCallback.SCOPE,
            )
            require(quiche_enable_debug_logging(cb, MemorySegment.NULL) == 0) {
                "Cannot enable quiche debug logging"
            }
        }
    }

    private class LoggingCallback {
        companion object Companion {
            val INSTANCE: LoggingCallback = LoggingCallback()
            val SCOPE: Arena = Arena.ofAuto()
        }

        // Called from native Quiche library
        fun log(msg: MemorySegment, argp: MemorySegment) {
            logger.debug { "[Quiche] ${msg.getUtf8String(0L)}" }
        }
    }

    fun quiche_version(): MemorySegment {
        return DowncallHandles.quiche_version.invokeExact() as MemorySegment
    }

    fun quiche_enable_debug_logging(cb: MemorySegment, argp: MemorySegment): Int {
        return DowncallHandles.quiche_enable_debug_logging.invokeExact(cb, argp) as Int
    }

    fun quiche_config_new(version: Int): MemorySegment? {
        return DowncallHandles.quiche_config_new.invokeExact(version) as? MemorySegment
    }

    fun quiche_config_load_cert_chain_from_pem_file(config: MemorySegment, path: MemorySegment): Int {
        return DowncallHandles.quiche_config_load_cert_chain_from_pem_file.invokeExact(config, path) as Int
    }

    fun quiche_config_load_priv_key_from_pem_file(config: MemorySegment, path: MemorySegment): Int {
        return DowncallHandles.quiche_config_load_priv_key_from_pem_file.invokeExact(config, path) as Int
    }

    fun quiche_config_load_verify_locations_from_file(config: MemorySegment, path: MemorySegment): Int {
        return DowncallHandles.quiche_config_load_verify_locations_from_file.invokeExact(config, path) as Int
    }

    fun quiche_config_load_verify_locations_from_directory(config: MemorySegment, path: MemorySegment): Int {
        return DowncallHandles.quiche_config_load_verify_locations_from_directory.invokeExact(config, path) as Int
    }

    fun quiche_config_verify_peer(config: MemorySegment, v: Boolean) {
        DowncallHandles.quiche_config_verify_peer.invokeExact(config, (if (v) 1 else 0).toByte())
    }

    fun quiche_config_grease(config: MemorySegment, v: Boolean) {
        DowncallHandles.quiche_config_grease.invokeExact(config, (if (v) 1 else 0).toByte())
    }

    fun quiche_config_log_keys(config: MemorySegment) {
        DowncallHandles.quiche_config_log_keys.invokeExact(config)
    }

    fun quiche_config_enable_early_data(config: MemorySegment) {
        DowncallHandles.quiche_config_enable_early_data.invokeExact(config)
    }

    fun quiche_config_set_application_protos(config: MemorySegment, protos: MemorySegment, protos_len: Long): Int {
        return DowncallHandles.quiche_config_set_application_protos.invokeExact(config, protos, protos_len) as Int
    }

    fun quiche_config_set_max_idle_timeout(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_max_idle_timeout.invokeExact(config, v)
    }

    fun quiche_config_set_max_recv_udp_payload_size(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_max_recv_udp_payload_size.invokeExact(config, v)
    }

    fun quiche_config_set_max_send_udp_payload_size(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_max_send_udp_payload_size.invokeExact(config, v)
    }

    fun quiche_config_set_initial_max_data(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_initial_max_data.invokeExact(config, v)
    }

    fun quiche_config_set_initial_max_stream_data_bidi_local(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_initial_max_stream_data_bidi_local.invokeExact(config, v)
    }

    fun quiche_config_set_initial_max_stream_data_bidi_remote(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_initial_max_stream_data_bidi_remote.invokeExact(config, v)
    }

    fun quiche_config_set_initial_max_stream_data_uni(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_initial_max_stream_data_uni.invokeExact(config, v)
    }

    fun quiche_config_set_initial_max_streams_bidi(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_initial_max_streams_bidi.invokeExact(config, v)
    }

    fun quiche_config_set_initial_max_streams_uni(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_initial_max_streams_uni.invokeExact(config, v)
    }

    fun quiche_config_set_ack_delay_exponent(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_ack_delay_exponent.invokeExact(config, v)
    }

    fun quiche_config_set_max_ack_delay(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_max_ack_delay.invokeExact(config, v)
    }

    fun quiche_config_set_disable_active_migration(config: MemorySegment, v: Boolean) {
        DowncallHandles.quiche_config_set_disable_active_migration.invokeExact(config, (if (v) 1 else 0).toByte())
    }

    fun quiche_config_set_cc_algorithm_name(config: MemorySegment, algo: MemorySegment): Int {
        return DowncallHandles.quiche_config_set_cc_algorithm_name.invokeExact(config, algo) as Int
    }

    fun quiche_config_set_initial_congestion_window_packets(config: MemorySegment, packets: Long) {
        DowncallHandles.quiche_config_set_initial_congestion_window_packets.invokeExact(config, packets)
    }

    fun quiche_config_set_cc_algorithm(config: MemorySegment, algo: Int) {
        DowncallHandles.quiche_config_set_cc_algorithm.invokeExact(config, algo)
    }

    fun quiche_config_enable_hystart(config: MemorySegment, v: Boolean) {
        DowncallHandles.quiche_config_enable_hystart.invokeExact(config, (if (v) 1 else 0).toByte())
    }

    fun quiche_config_enable_pacing(config: MemorySegment, v: Boolean) {
        DowncallHandles.quiche_config_enable_pacing.invokeExact(config, (if (v) 1 else 0).toByte())
    }

    fun quiche_config_set_max_pacing_rate(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_max_pacing_rate.invokeExact(config, v)
    }

    fun quiche_config_enable_dgram(config: MemorySegment, enabled: Boolean, recv_queue_len: Long, send_queue_len: Long) {
        DowncallHandles.quiche_config_enable_dgram.invokeExact(config, (if (enabled) 1 else 0).toByte(), recv_queue_len, send_queue_len)
    }

    fun quiche_config_set_max_connection_window(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_max_connection_window.invokeExact(config, v)
    }

    fun quiche_config_set_max_stream_window(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_max_stream_window.invokeExact(config, v)
    }

    fun quiche_config_set_active_connection_id_limit(config: MemorySegment, v: Long) {
        DowncallHandles.quiche_config_set_active_connection_id_limit.invokeExact(config, v)
    }

    fun quiche_config_set_stateless_reset_token(config: MemorySegment, v: MemorySegment) {
        DowncallHandles.quiche_config_set_stateless_reset_token.invokeExact(config, v)
    }

    fun quiche_config_set_disable_dcid_reuse(config: MemorySegment, v: Boolean) {
        DowncallHandles.quiche_config_set_disable_dcid_reuse.invokeExact(config, (if (v) 1 else 0).toByte())
    }

    fun quiche_config_set_ticket_key(config: MemorySegment, key: MemorySegment, key_len: Long): Int {
        return DowncallHandles.quiche_config_set_ticket_key.invokeExact(config, key, key_len) as Int
    }

    fun quiche_config_free(config: MemorySegment) {
        DowncallHandles.quiche_config_free.invokeExact(config)
    }

    fun quiche_header_info(buf: MemorySegment, bufLen: Long, dcil: Long, version: MemorySegment, type: MemorySegment, scid: MemorySegment, scidLen: MemorySegment, dcid: MemorySegment, dcidLen: MemorySegment, token: MemorySegment, token_len: MemorySegment): Int {
        return DowncallHandles.quiche_header_info.invokeExact(buf, bufLen, dcil, version, type, scid, scidLen, dcid, dcidLen, token, token_len) as Int
    }

    fun quiche_accept(scid: MemorySegment, scidLen: Long, odcid: MemorySegment, odcid_len: Long, local: MemorySegment, localLen: Int, peer: MemorySegment, peerLen: Int, config: MemorySegment): MemorySegment? {
        return DowncallHandles.quiche_accept.invokeExact(scid, scidLen, odcid, odcid_len, local, localLen, peer, peerLen, config) as MemorySegment?
    }

    fun quiche_connect(server_name: MemorySegment, scid: MemorySegment, scidLen: Long, local: MemorySegment, localLen: Int, peer: MemorySegment, peerLen: Int, config: MemorySegment): MemorySegment? {
        return DowncallHandles.quiche_connect.invokeExact(server_name, scid, scidLen, local, localLen, peer, peerLen, config) as MemorySegment?
    }

    fun quiche_negotiate_version(scid: MemorySegment, scidLen: Long, dcid: MemorySegment, dcidLen: Long, out: MemorySegment, outLen: Long): Long {
        return DowncallHandles.quiche_negotiate_version.invokeExact(scid, scidLen, dcid, dcidLen, out, outLen) as Long
    }

    fun quiche_retry(scid: MemorySegment, scidLen: Long, dcid: MemorySegment, dcidLen: Long, new_scid: MemorySegment, new_scid_len: Long, token: MemorySegment, token_len: Long, version: Int, out: MemorySegment, outLen: Long): Long {
        return DowncallHandles.quiche_retry.invokeExact(scid, scidLen, dcid, dcidLen, new_scid, new_scid_len, token, token_len, version, out, outLen) as Long
    }

    fun quiche_version_is_supported(version: Int): Boolean {
        return (DowncallHandles.quiche_version_is_supported.invokeExact(version) as Byte).toInt() != 0
    }

    fun quiche_conn_new_with_tls(scid: MemorySegment, scidLen: Long, odcid: MemorySegment, odcid_len: Long, local: MemorySegment, localLen: Int, peer: MemorySegment, peerLen: Int, config: MemorySegment, ssl: MemorySegment, is_server: Boolean): MemorySegment? {
        return DowncallHandles.quiche_conn_new_with_tls.invokeExact(scid, scidLen, odcid, odcid_len, local, localLen, peer, peerLen, config, ssl, (if (is_server) 1 else 0).toByte()) as MemorySegment?
    }

    fun quiche_conn_set_keylog_path(conn: MemorySegment, path: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_set_keylog_path.invokeExact(conn, path) as Byte).toInt() != 0
    }

    fun quiche_conn_set_keylog_fd(conn: MemorySegment, fd: Int) {
        DowncallHandles.quiche_conn_set_keylog_fd.invokeExact(conn, fd)
    }

    fun quiche_conn_set_qlog_path(conn: MemorySegment, path: MemorySegment, log_title: MemorySegment, log_desc: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_set_qlog_path.invokeExact(conn, path, log_title, log_desc) as Byte).toInt() != 0
    }

    fun quiche_conn_set_qlog_fd(conn: MemorySegment, fd: Int, log_title: MemorySegment, log_desc: MemorySegment) {
        DowncallHandles.quiche_conn_set_qlog_fd.invokeExact(conn, fd, log_title, log_desc)
    }

    fun quiche_conn_set_session(conn: MemorySegment, buf: MemorySegment, bufLen: Long): Int {
        return DowncallHandles.quiche_conn_set_session.invokeExact(conn, buf, bufLen) as Int
    }

    fun quiche_conn_recv(conn: MemorySegment, buf: MemorySegment, bufLen: Long, info: MemorySegment): Long {
        return DowncallHandles.quiche_conn_recv.invokeExact(conn, buf, bufLen, info) as Long
    }

    fun quiche_conn_send(conn: MemorySegment, out: MemorySegment, outLen: Long, out_info: MemorySegment): Long {
        return DowncallHandles.quiche_conn_send.invokeExact(conn, out, outLen, out_info) as Long
    }

    fun quiche_conn_send_quantum(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_send_quantum.invokeExact(conn) as Long
    }

    fun quiche_conn_send_on_path(conn: MemorySegment, out: MemorySegment, outLen: Long, from: MemorySegment, from_len: Int, to: MemorySegment, to_len: Int, out_info: MemorySegment): Long {
        return DowncallHandles.quiche_conn_send_on_path.invokeExact(conn, out, outLen, from, from_len, to, to_len, out_info) as Long
    }

    fun quiche_conn_send_quantum_on_path(conn: MemorySegment, local_addr: MemorySegment, localLen: Int, peer_addr: MemorySegment, peerLen: Int): Long {
        return DowncallHandles.quiche_conn_send_quantum_on_path.invokeExact(conn, local_addr, localLen, peer_addr, peerLen) as Long
    }

    fun quiche_conn_stream_recv(conn: MemorySegment, streamId: Long, out: MemorySegment, bufLen: Long, fin: MemorySegment, out_error_code: MemorySegment): Long {
        return DowncallHandles.quiche_conn_stream_recv.invokeExact(conn, streamId, out, bufLen, fin, out_error_code) as Long
    }

    fun quiche_conn_stream_send(conn: MemorySegment, streamId: Long, buf: MemorySegment, bufLen: Long, fin: Boolean, out_error_code: MemorySegment): Long {
        return DowncallHandles.quiche_conn_stream_send.invokeExact(conn, streamId, buf, bufLen, (if (fin) 1 else 0).toByte(), out_error_code) as Long
    }

    fun quiche_conn_stream_priority(conn: MemorySegment, streamId: Long, urgency: Byte, incremental: Boolean): Int {
        return DowncallHandles.quiche_conn_stream_priority.invokeExact(conn, streamId, urgency, (if (incremental) 1 else 0).toByte()) as Int
    }

    fun quiche_conn_stream_shutdown(conn: MemorySegment, streamId: Long, direction: Int, err: Long): Int {
        return DowncallHandles.quiche_conn_stream_shutdown.invokeExact(conn, streamId, direction, err) as Int
    }

    fun quiche_conn_stream_capacity(conn: MemorySegment, streamId: Long): Long {
        return DowncallHandles.quiche_conn_stream_capacity.invokeExact(conn, streamId) as Long
    }

    fun quiche_conn_stream_readable(conn: MemorySegment, streamId: Long): Boolean {
        return (DowncallHandles.quiche_conn_stream_readable.invokeExact(conn, streamId) as Byte).toInt() != 0
    }

    fun quiche_conn_stream_readable_next(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_stream_readable_next.invokeExact(conn) as Long
    }

    fun quiche_conn_stream_writable(conn: MemorySegment, streamId: Long, len: Long): Int {
        return DowncallHandles.quiche_conn_stream_writable.invokeExact(conn, streamId, len) as Int
    }

    fun quiche_conn_stream_writable_next(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_stream_writable_next.invokeExact(conn) as Long
    }

    fun quiche_conn_stream_finished(conn: MemorySegment, streamId: Long): Boolean {
        return (DowncallHandles.quiche_conn_stream_finished.invokeExact(conn, streamId) as Byte).toInt() != 0
    }

    fun quiche_conn_readable(conn: MemorySegment): MemorySegment {
        return DowncallHandles.quiche_conn_readable.invokeExact(conn) as MemorySegment
    }

    fun quiche_conn_writable(conn: MemorySegment): MemorySegment {
        return DowncallHandles.quiche_conn_writable.invokeExact(conn) as MemorySegment
    }

    fun quiche_conn_max_send_udp_payload_size(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_max_send_udp_payload_size.invokeExact(conn) as Long
    }

    fun quiche_conn_timeout_as_nanos(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_timeout_as_nanos.invokeExact(conn) as Long
    }

    fun quiche_conn_timeout_as_millis(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_timeout_as_millis.invokeExact(conn) as Long
    }

    fun quiche_conn_on_timeout(conn: MemorySegment) {
        DowncallHandles.quiche_conn_on_timeout.invokeExact(conn)
    }

    fun quiche_conn_close(conn: MemorySegment, app: Boolean, err: Long, reason: MemorySegment, reason_len: Long): Int {
        return DowncallHandles.quiche_conn_close.invokeExact(conn, (if (app) 1 else 0).toByte(), err, reason, reason_len) as Int
    }

    fun quiche_conn_trace_id(conn: MemorySegment, out: MemorySegment, outLen: MemorySegment) {
        DowncallHandles.quiche_conn_trace_id.invokeExact(conn, out, outLen)
    }

    fun quiche_conn_source_id(conn: MemorySegment, out: MemorySegment, outLen: MemorySegment) {
        DowncallHandles.quiche_conn_source_id.invokeExact(conn, out, outLen)
    }

    fun quiche_conn_source_ids(conn: MemorySegment): MemorySegment? {
        return DowncallHandles.quiche_conn_source_ids.invokeExact(conn) as MemorySegment?
    }

    fun quiche_connection_id_iter_next(iter: MemorySegment, out: MemorySegment, outLen: MemorySegment): Boolean {
        return (DowncallHandles.quiche_connection_id_iter_next.invokeExact(iter, out, outLen) as Byte).toInt() != 0
    }

    fun quiche_connection_id_iter_free(iter: MemorySegment) {
        DowncallHandles.quiche_connection_id_iter_free.invokeExact(iter)
    }

    fun quiche_conn_destination_id(conn: MemorySegment, out: MemorySegment, outLen: MemorySegment) {
        DowncallHandles.quiche_conn_destination_id.invokeExact(conn, out, outLen)
    }

    fun quiche_conn_application_proto(conn: MemorySegment, out: MemorySegment, outLen: MemorySegment) {
        DowncallHandles.quiche_conn_application_proto.invokeExact(conn, out, outLen)
    }

    fun quiche_conn_peer_cert(conn: MemorySegment, out: MemorySegment, outLen: MemorySegment) {
        DowncallHandles.quiche_conn_peer_cert.invokeExact(conn, out, outLen)
    }

    fun quiche_conn_session(conn: MemorySegment, out: MemorySegment, outLen: MemorySegment) {
        DowncallHandles.quiche_conn_session.invokeExact(conn, out, outLen)
    }

    fun quiche_conn_is_established(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_established.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_is_resumed(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_resumed.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_is_in_early_data(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_in_early_data.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_is_readable(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_readable.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_is_draining(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_draining.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_peer_streams_left_bidi(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_peer_streams_left_bidi.invokeExact(conn) as Long
    }

    fun quiche_conn_peer_streams_left_uni(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_peer_streams_left_uni.invokeExact(conn) as Long
    }

    fun quiche_conn_is_closed(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_closed.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_is_timed_out(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_timed_out.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_peer_error(conn: MemorySegment, is_app: MemorySegment, error_code: MemorySegment, reason: MemorySegment, reason_len: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_peer_error.invokeExact(conn, is_app, error_code, reason, reason_len) as Byte).toInt() != 0
    }

    fun quiche_conn_local_error(conn: MemorySegment, is_app: MemorySegment, error_code: MemorySegment, reason: MemorySegment, reason_len: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_local_error.invokeExact(conn, is_app, error_code, reason, reason_len) as Byte).toInt() != 0
    }

    fun quiche_stream_iter_next(iter: MemorySegment, streamId: MemorySegment): Boolean {
        return (DowncallHandles.quiche_stream_iter_next.invokeExact(iter, streamId) as Byte).toInt() != 0
    }

    fun quiche_stream_iter_free(iter: MemorySegment) {
        DowncallHandles.quiche_stream_iter_free.invokeExact(iter)
    }

    fun quiche_conn_stats(conn: MemorySegment, out: MemorySegment) {
        DowncallHandles.quiche_conn_stats.invokeExact(conn, out)
    }

    fun quiche_conn_peer_transport_params(conn: MemorySegment, out: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_peer_transport_params.invokeExact(conn, out) as Byte).toInt() != 0
    }

    fun quiche_conn_path_stats(conn: MemorySegment, idx: Long, out: MemorySegment): Int {
        return DowncallHandles.quiche_conn_path_stats.invokeExact(conn, idx, out) as Int
    }

    fun quiche_conn_is_server(conn: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_is_server.invokeExact(conn) as Byte).toInt() != 0
    }

    fun quiche_conn_send_ack_eliciting(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_send_ack_eliciting.invokeExact(conn) as Long
    }

    fun quiche_conn_send_ack_eliciting_on_path(conn: MemorySegment, local: MemorySegment, localLen: Int, peer: MemorySegment, peerLen: Int): Long {
        return DowncallHandles.quiche_conn_send_ack_eliciting_on_path.invokeExact(conn, local, localLen, peer, peerLen) as Long
    }

    fun quiche_conn_retired_scid_next(conn: MemorySegment, out: MemorySegment, outLen: MemorySegment): Boolean {
        return (DowncallHandles.quiche_conn_retired_scid_next.invokeExact(conn, out, outLen) as Byte).toInt() != 0
    }

    fun quiche_conn_retired_scids(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_retired_scids.invokeExact(conn) as Long
    }

    fun quiche_conn_available_dcids(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_available_dcids.invokeExact(conn) as Long
    }

    fun quiche_conn_scids_left(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_scids_left.invokeExact(conn) as Long
    }

    fun quiche_conn_active_scids(conn: MemorySegment): Long {
        return DowncallHandles.quiche_conn_active_scids.invokeExact(conn) as Long
    }

    fun quiche_conn_new_scid(conn: MemorySegment, scid: MemorySegment, scidLen: Long, reset_token: MemorySegment, retire_if_needed: Boolean, scid_seq: MemorySegment): Int {
        return DowncallHandles.quiche_conn_new_scid.invokeExact(conn, scid, scidLen, reset_token, (if (retire_if_needed) 1 else 0).toByte(), scid_seq) as Int
    }

    fun quiche_conn_probe_path(conn: MemorySegment, local: MemorySegment, localLen: Int, peer: MemorySegment, peerLen: Int, seq: MemorySegment): Int {
        return DowncallHandles.quiche_conn_probe_path.invokeExact(conn, local, localLen, peer, peerLen, seq) as Int
    }

    fun quiche_conn_migrate_source(conn: MemorySegment, local: MemorySegment, localLen: Int, seq: MemorySegment): Int {
        return DowncallHandles.quiche_conn_migrate_source.invokeExact(conn, local, localLen, seq) as Int
    }

    fun quiche_conn_migrate(conn: MemorySegment, local: MemorySegment, localLen: Int, peer: MemorySegment, peerLen: Int, seq: MemorySegment): Int {
        return DowncallHandles.quiche_conn_migrate.invokeExact(conn, local, localLen, peer, peerLen, seq) as Int
    }

    fun quiche_conn_path_event_next(conn: MemorySegment): MemorySegment? {
        return DowncallHandles.quiche_conn_path_event_next.invokeExact(conn) as MemorySegment?
    }

    fun quiche_path_event_type(ev: MemorySegment): Int {
        return DowncallHandles.quiche_path_event_type.invokeExact(ev) as Int
    }

    fun quiche_path_event_new(ev: MemorySegment, local: MemorySegment, localLen: MemorySegment, peer: MemorySegment, peerLen: MemorySegment) {
        DowncallHandles.quiche_path_event_new.invokeExact(ev, local, localLen, peer, peerLen)
    }

    fun quiche_path_event_validated(ev: MemorySegment, local: MemorySegment, localLen: MemorySegment, peer: MemorySegment, peerLen: MemorySegment) {
        DowncallHandles.quiche_path_event_validated.invokeExact(ev, local, localLen, peer, peerLen)
    }

    fun quiche_path_event_failed_validation(ev: MemorySegment, local: MemorySegment, localLen: MemorySegment, peer: MemorySegment, peerLen: MemorySegment) {
        DowncallHandles.quiche_path_event_failed_validation.invokeExact(ev, local, localLen, peer, peerLen)
    }

    fun quiche_path_event_closed(ev: MemorySegment, local: MemorySegment, localLen: MemorySegment, peer: MemorySegment, peerLen: MemorySegment) {
        DowncallHandles.quiche_path_event_closed.invokeExact(ev, local, localLen, peer, peerLen)
    }

    fun quiche_path_event_reused_source_connection_id(ev: MemorySegment, id: MemorySegment, old_local: MemorySegment, old_local_len: MemorySegment, old_peer: MemorySegment, old_peer_len: MemorySegment, local: MemorySegment, localLen: MemorySegment, peer: MemorySegment, peerLen: MemorySegment) {
        DowncallHandles.quiche_path_event_reused_source_connection_id.invokeExact(ev, id, old_local, old_local_len, old_peer, old_peer_len, local, localLen, peer, peerLen)
    }

    fun quiche_path_event_peer_migrated(ev: MemorySegment, local: MemorySegment, localLen: MemorySegment, peer: MemorySegment, peerLen: MemorySegment) {
        DowncallHandles.quiche_path_event_peer_migrated.invokeExact(ev, local, localLen, peer, peerLen)
    }

    fun quiche_path_event_free(ev: MemorySegment) {
        DowncallHandles.quiche_path_event_free.invokeExact(ev)
    }

    fun quiche_conn_retire_dcid(conn: MemorySegment, dcid_seq: Long): Int {
        return DowncallHandles.quiche_conn_retire_dcid.invokeExact(conn, dcid_seq) as Int
    }

    fun quiche_conn_paths_iter(conn: MemorySegment, from: MemorySegment, from_len: Long): MemorySegment? {
        return DowncallHandles.quiche_conn_paths_iter.invokeExact(conn, from, from_len) as MemorySegment?
    }

    fun quiche_socket_addr_iter_next(iter: MemorySegment, peer: MemorySegment, peerLen: MemorySegment): Boolean {
        return (DowncallHandles.quiche_socket_addr_iter_next.invokeExact(iter, peer, peerLen) as Byte).toInt() != 0
    }

    fun quiche_socket_addr_iter_free(iter: MemorySegment) {
        DowncallHandles.quiche_socket_addr_iter_free.invokeExact(iter)
    }

    fun quiche_conn_is_path_validated(conn: MemorySegment, from: MemorySegment, from_len: Long, to: MemorySegment, to_len: Long): Int {
        return DowncallHandles.quiche_conn_is_path_validated.invokeExact(conn, from, from_len, to, to_len) as Int
    }

    fun quiche_conn_free(conn: MemorySegment) {
        DowncallHandles.quiche_conn_free.invokeExact(conn)
    }

    fun quiche_put_varint(buf: MemorySegment, bufLen: Long, `val`: Long): Int {
        return DowncallHandles.quiche_put_varint.invokeExact(buf, bufLen, `val`) as Int
    }

    fun quiche_get_varint(buf: MemorySegment, bufLen: Long, `val`: MemorySegment): Long {
        return DowncallHandles.quiche_get_varint.invokeExact(buf, bufLen, `val`) as Long
    }

    private object DowncallHandles {
        val quiche_version = NativeHelper.downcallHandle(
            "quiche_version",
            FunctionDescriptor.of(C_POINTER),
        )
        val quiche_enable_debug_logging = NativeHelper.downcallHandle(
            "quiche_enable_debug_logging",
            FunctionDescriptor.of(
                C_INT,
                NativeHelper.C_POINTER,
                NativeHelper.C_POINTER,
            ),
        )
        val quiche_config_new = NativeHelper.downcallHandle(
            "quiche_config_new",
            FunctionDescriptor.of(
                C_POINTER,
                C_INT,
            ),
        )
        val quiche_config_load_cert_chain_from_pem_file = NativeHelper.downcallHandle(
            "quiche_config_load_cert_chain_from_pem_file",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_config_load_priv_key_from_pem_file = NativeHelper.downcallHandle(
            "quiche_config_load_priv_key_from_pem_file",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_config_load_verify_locations_from_file = NativeHelper.downcallHandle(
            "quiche_config_load_verify_locations_from_file",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_config_load_verify_locations_from_directory = NativeHelper.downcallHandle(
            "quiche_config_load_verify_locations_from_directory",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_config_verify_peer = NativeHelper.downcallHandle(
            "quiche_config_verify_peer",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_BOOL,
            ),
        )
        val quiche_config_grease = NativeHelper.downcallHandle(
            "quiche_config_grease",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_BOOL,
            ),
        )

        val quiche_config_log_keys = NativeHelper.downcallHandle(
            "quiche_config_log_keys",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )

        val quiche_config_enable_early_data = NativeHelper.downcallHandle(
            "quiche_config_enable_early_data",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_config_set_application_protos = NativeHelper.downcallHandle(
            "quiche_config_set_application_protos",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_max_idle_timeout = NativeHelper.downcallHandle(
            "quiche_config_set_max_idle_timeout",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_max_recv_udp_payload_size = NativeHelper.downcallHandle(
            "quiche_config_set_max_recv_udp_payload_size",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_max_send_udp_payload_size = NativeHelper.downcallHandle(
            "quiche_config_set_max_send_udp_payload_size",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_initial_max_data = NativeHelper.downcallHandle(
            "quiche_config_set_initial_max_data",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_initial_max_stream_data_bidi_local = NativeHelper.downcallHandle(
            "quiche_config_set_initial_max_stream_data_bidi_local",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_initial_max_stream_data_bidi_remote = NativeHelper.downcallHandle(
            "quiche_config_set_initial_max_stream_data_bidi_remote",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_initial_max_stream_data_uni = NativeHelper.downcallHandle(
            "quiche_config_set_initial_max_stream_data_uni",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_initial_max_streams_bidi = NativeHelper.downcallHandle(
            "quiche_config_set_initial_max_streams_bidi",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_initial_max_streams_uni = NativeHelper.downcallHandle(
            "quiche_config_set_initial_max_streams_uni",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_ack_delay_exponent = NativeHelper.downcallHandle(
            "quiche_config_set_ack_delay_exponent",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )

        val quiche_config_set_max_ack_delay = NativeHelper.downcallHandle(
            "quiche_config_set_max_ack_delay",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_disable_active_migration = NativeHelper.downcallHandle(
            "quiche_config_set_disable_active_migration",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_BOOL,
            ),
        )
        val quiche_config_set_cc_algorithm_name = NativeHelper.downcallHandle(
            "quiche_config_set_cc_algorithm_name",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_config_set_initial_congestion_window_packets = NativeHelper.downcallHandle(
            "quiche_config_set_initial_congestion_window_packets",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_cc_algorithm = NativeHelper.downcallHandle(
            "quiche_config_set_cc_algorithm",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_INT,
            ),
        )
        val quiche_config_enable_hystart = NativeHelper.downcallHandle(
            "quiche_config_enable_hystart",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_BOOL,
            ),
        )
        val quiche_config_enable_pacing = NativeHelper.downcallHandle(
            "quiche_config_enable_pacing",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_BOOL,
            ),
        )
        val quiche_config_set_max_pacing_rate = NativeHelper.downcallHandle(
            "quiche_config_set_max_pacing_rate",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_enable_dgram = NativeHelper.downcallHandle(
            "quiche_config_enable_dgram",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_BOOL,
                C_LONG,
                C_LONG,
            ),
        )
        val quiche_config_set_max_connection_window = NativeHelper.downcallHandle(
            "quiche_config_set_max_connection_window",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_max_stream_window = NativeHelper.downcallHandle(
            "quiche_config_set_max_stream_window",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_active_connection_id_limit = NativeHelper.downcallHandle(
            "quiche_config_set_active_connection_id_limit",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_set_stateless_reset_token = NativeHelper.downcallHandle(
            "quiche_config_set_stateless_reset_token",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_config_set_disable_dcid_reuse = NativeHelper.downcallHandle(
            "quiche_config_set_disable_dcid_reuse",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_BOOL,
            ),
        )
        val quiche_config_set_ticket_key = NativeHelper.downcallHandle(
            "quiche_config_set_ticket_key",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_config_free = NativeHelper.downcallHandle(
            "quiche_config_free",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_header_info = NativeHelper.downcallHandle(
            "quiche_header_info",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_LONG,
                C_LONG,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_accept = NativeHelper.downcallHandle(
            "quiche_accept",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
                C_POINTER,
            ),
        )
        val quiche_connect = NativeHelper.downcallHandle(
            "quiche_connect",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
                C_POINTER,
            ),
        )
        val quiche_negotiate_version = NativeHelper.downcallHandle(
            "quiche_negotiate_version",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_retry = NativeHelper.downcallHandle(
            "quiche_retry",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_INT,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_version_is_supported = NativeHelper.downcallHandle(
            "quiche_version_is_supported",
            FunctionDescriptor.of(
                C_BOOL,
                C_INT,
            ),
        )
        val quiche_conn_new_with_tls = NativeHelper.downcallHandle(
            "quiche_conn_new_with_tls",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_POINTER,
                C_BOOL,
            ),
        )
        val quiche_conn_set_keylog_path = NativeHelper.downcallHandle(
            "quiche_conn_set_keylog_path",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
            ),
        )

        val quiche_conn_set_keylog_fd = NativeHelper.downcallHandle(
            "quiche_conn_set_keylog_fd",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_INT,
            ),
        )
        val quiche_conn_set_qlog_path = NativeHelper.downcallHandle(
            "quiche_conn_set_qlog_path",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_set_qlog_fd = NativeHelper.downcallHandle(
            "quiche_conn_set_qlog_fd",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_INT,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_set_session = NativeHelper.downcallHandle(
            "quiche_conn_set_session",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_conn_recv = NativeHelper.downcallHandle(
            "quiche_conn_recv",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_send = NativeHelper.downcallHandle(
            "quiche_conn_send",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_send_quantum = NativeHelper.downcallHandle(
            "quiche_conn_send_quantum",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_send_on_path = NativeHelper.downcallHandle(
            "quiche_conn_send_on_path",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
                C_POINTER,
            ),
        )
        val quiche_conn_send_quantum_on_path = NativeHelper.downcallHandle(
            "quiche_conn_send_quantum_on_path",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
            ),
        )
        val quiche_conn_stream_recv = NativeHelper.downcallHandle(
            "quiche_conn_stream_recv",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_stream_send = NativeHelper.downcallHandle(
            "quiche_conn_stream_send",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_stream_priority = NativeHelper.downcallHandle(
            "quiche_conn_stream_priority",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_LONG,
                C_BYTE,
                C_BOOL,
            ),
        )
        val quiche_conn_stream_shutdown = NativeHelper.downcallHandle(
            "quiche_conn_stream_shutdown",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_LONG,
                C_INT,
                C_LONG,
            ),
        )
        val quiche_conn_stream_capacity = NativeHelper.downcallHandle(
            "quiche_conn_stream_capacity",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_conn_stream_readable = NativeHelper.downcallHandle(
            "quiche_conn_stream_readable",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_conn_stream_readable_next = NativeHelper.downcallHandle(
            "quiche_conn_stream_readable_next",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_stream_writable = NativeHelper.downcallHandle(
            "quiche_conn_stream_writable",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_LONG,
                C_LONG,
            ),
        )
        val quiche_conn_stream_writable_next = NativeHelper.downcallHandle(
            "quiche_conn_stream_writable_next",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_stream_finished = NativeHelper.downcallHandle(
            "quiche_conn_stream_finished",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_conn_readable = NativeHelper.downcallHandle(
            "quiche_conn_readable",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_writable = NativeHelper.downcallHandle(
            "quiche_conn_writable",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_max_send_udp_payload_size = NativeHelper.downcallHandle(
            "quiche_conn_max_send_udp_payload_size",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_timeout_as_nanos = NativeHelper.downcallHandle(
            "quiche_conn_timeout_as_nanos",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_timeout_as_millis = NativeHelper.downcallHandle(
            "quiche_conn_timeout_as_millis",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_on_timeout = NativeHelper.downcallHandle(
            "quiche_conn_on_timeout",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_conn_close = NativeHelper.downcallHandle(
            "quiche_conn_close",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_BOOL,
                C_LONG,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_conn_trace_id = NativeHelper.downcallHandle(
            "quiche_conn_trace_id",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_source_id = NativeHelper.downcallHandle(
            "quiche_conn_source_id",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_source_ids = NativeHelper.downcallHandle(
            "quiche_conn_source_ids",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_connection_id_iter_next = NativeHelper.downcallHandle(
            "quiche_connection_id_iter_next",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_connection_id_iter_free = NativeHelper.downcallHandle(
            "quiche_connection_id_iter_free",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_conn_destination_id = NativeHelper.downcallHandle(
            "quiche_conn_destination_id",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_application_proto = NativeHelper.downcallHandle(
            "quiche_conn_application_proto",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_peer_cert = NativeHelper.downcallHandle(
            "quiche_conn_peer_cert",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_session = NativeHelper.downcallHandle(
            "quiche_conn_session",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_is_established = NativeHelper.downcallHandle(
            "quiche_conn_is_established",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_is_resumed = NativeHelper.downcallHandle(
            "quiche_conn_is_resumed",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_is_in_early_data = NativeHelper.downcallHandle(
            "quiche_conn_is_in_early_data",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_is_readable = NativeHelper.downcallHandle(
            "quiche_conn_is_readable",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_is_draining = NativeHelper.downcallHandle(
            "quiche_conn_is_draining",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_peer_streams_left_bidi = NativeHelper.downcallHandle(
            "quiche_conn_peer_streams_left_bidi",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_peer_streams_left_uni = NativeHelper.downcallHandle(
            "quiche_conn_peer_streams_left_uni",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_is_closed = NativeHelper.downcallHandle(
            "quiche_conn_is_closed",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_is_timed_out = NativeHelper.downcallHandle(
            "quiche_conn_is_timed_out",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_peer_error = NativeHelper.downcallHandle(
            "quiche_conn_peer_error",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_local_error = NativeHelper.downcallHandle(
            "quiche_conn_local_error",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_stream_iter_next = NativeHelper.downcallHandle(
            "quiche_stream_iter_next",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_stream_iter_free = NativeHelper.downcallHandle(
            "quiche_stream_iter_free",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_conn_stats = NativeHelper.downcallHandle(
            "quiche_conn_stats",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_peer_transport_params = NativeHelper.downcallHandle(
            "quiche_conn_peer_transport_params",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_path_stats = NativeHelper.downcallHandle(
            "quiche_conn_path_stats",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_is_server = NativeHelper.downcallHandle(
            "quiche_conn_is_server",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_send_ack_eliciting = NativeHelper.downcallHandle(
            "quiche_conn_send_ack_eliciting",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_send_ack_eliciting_on_path = NativeHelper.downcallHandle(
            "quiche_conn_send_ack_eliciting_on_path",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
            ),
        )
        val quiche_conn_retired_scid_next = NativeHelper.downcallHandle(
            "quiche_conn_retired_scid_next",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_conn_retired_scids = NativeHelper.downcallHandle(
            "quiche_conn_retired_scids",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_available_dcids = NativeHelper.downcallHandle(
            "quiche_conn_available_dcids",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_scids_left = NativeHelper.downcallHandle(
            "quiche_conn_scids_left",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_active_scids = NativeHelper.downcallHandle(
            "quiche_conn_active_scids",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
            ),
        )
        val quiche_conn_new_scid = NativeHelper.downcallHandle(
            "quiche_conn_new_scid",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_BOOL,
                C_POINTER,
            ),
        )
        val quiche_conn_probe_path = NativeHelper.downcallHandle(
            "quiche_conn_probe_path",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
                C_POINTER,
            ),
        )
        val quiche_conn_migrate_source = NativeHelper.downcallHandle(
            "quiche_conn_migrate_source",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_INT,
                C_POINTER,
            ),
        )
        val quiche_conn_migrate = NativeHelper.downcallHandle(
            "quiche_conn_migrate",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_INT,
                C_POINTER,
                C_INT,
                C_POINTER,
            ),
        )
        val quiche_conn_path_event_next = NativeHelper.downcallHandle(
            "quiche_conn_path_event_next",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_path_event_type = NativeHelper.downcallHandle(
            "quiche_path_event_type",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
            ),
        )
        val quiche_path_event_new = NativeHelper.downcallHandle(
            "quiche_path_event_new",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_path_event_validated = NativeHelper.downcallHandle(
            "quiche_path_event_validated",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_path_event_failed_validation = NativeHelper.downcallHandle(
            "quiche_path_event_failed_validation",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_path_event_closed = NativeHelper.downcallHandle(
            "quiche_path_event_closed",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_path_event_reused_source_connection_id = NativeHelper.downcallHandle(
            "quiche_path_event_reused_source_connection_id",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_path_event_peer_migrated = NativeHelper.downcallHandle(
            "quiche_path_event_peer_migrated",
            FunctionDescriptor.ofVoid(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_path_event_free = NativeHelper.downcallHandle(
            "quiche_path_event_free",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_conn_retire_dcid = NativeHelper.downcallHandle(
            "quiche_conn_retire_dcid",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_conn_paths_iter = NativeHelper.downcallHandle(
            "quiche_conn_paths_iter",
            FunctionDescriptor.of(
                C_POINTER,
                C_POINTER,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_socket_addr_iter_next = NativeHelper.downcallHandle(
            "quiche_socket_addr_iter_next",
            FunctionDescriptor.of(
                C_BOOL,
                C_POINTER,
                C_POINTER,
                C_POINTER,
            ),
        )
        val quiche_socket_addr_iter_free = NativeHelper.downcallHandle(
            "quiche_socket_addr_iter_free",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_conn_is_path_validated = NativeHelper.downcallHandle(
            "quiche_conn_is_path_validated",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_POINTER,
                C_LONG,
                C_POINTER,
                C_LONG,
            ),
        )
        val quiche_conn_free = NativeHelper.downcallHandle(
            "quiche_conn_free",
            FunctionDescriptor.ofVoid(
                C_POINTER,
            ),
        )
        val quiche_put_varint = NativeHelper.downcallHandle(
            "quiche_put_varint",
            FunctionDescriptor.of(
                C_INT,
                C_POINTER,
                C_LONG,
                C_LONG,
            ),
        )
        val quiche_get_varint = NativeHelper.downcallHandle(
            "quiche_get_varint",
            FunctionDescriptor.of(
                C_LONG,
                C_POINTER,
                C_LONG,
                C_POINTER,
            ),
        )
    }
}
