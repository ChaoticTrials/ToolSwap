"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var coremods_1 = require("coremods");
function initializeCoreMod() {
    return {
        'toolswap': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.multiplayer.MultiPlayerGameMode',
                'methodName': 'm_105283_',
                'methodDesc': '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z'
            },
            'transformer': function (method) {
                var target = new coremods_1.InsnList();
                target.add(new coremods_1.VarInsnNode(coremods_1.Opcodes.ALOAD, 0));
                target.add(new coremods_1.VarInsnNode(coremods_1.Opcodes.ALOAD, 1));
                target.add(coremods_1.ASMAPI.buildMethodCall('de/melanx/toolswap/ClientToolSwap', 'switchStuff', '(Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;Lnet/minecraft/core/BlockPos;)V', coremods_1.ASMAPI.MethodType.STATIC));
                coremods_1.ASMAPI.log('WARN', "LOL", []);
                method.instructions.insert(target);
                return method;
            }
        }
    };
}
