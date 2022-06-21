import {ASMAPI, CoreMods, InsnList, MethodNode, Opcodes, VarInsnNode} from "coremods";

function initializeCoreMod(): CoreMods {
    return {
        'toolswap': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.multiplayer.MultiPlayerGameMode',
                'methodName': 'm_105283_',
                'methodDesc': '(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z'
            },
            'transformer': function (method: MethodNode) {
                const target = new InsnList();
                target.add(new VarInsnNode(Opcodes.ALOAD, 0));
                target.add(new VarInsnNode(Opcodes.ALOAD, 1));
                target.add(ASMAPI.buildMethodCall(
                    'de/melanx/toolswap/ClientToolSwap',
                    'searchForSwitching', '(Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;Lnet/minecraft/core/BlockPos;)V',
                    ASMAPI.MethodType.STATIC
                ));

                ASMAPI.log('WARN', "LOL", [])
                method.instructions.insert(target);
                return method;
            }
        }
    }
}
